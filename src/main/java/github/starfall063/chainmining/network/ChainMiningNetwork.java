package github.starfall063.chainmining.network;

import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.item.ItemChainStorageTerminal;
import github.starfall063.chainmining.util.ChainMiningStateManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

import java.util.ArrayList;
import java.util.List;

public final class ChainMiningNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    private ChainMiningNetwork() {

    }

    public static class ToggleStorageCoreMessage implements IMessage {
        int slot;
        int windowId;

        public ToggleStorageCoreMessage() {
        }

        public ToggleStorageCoreMessage(int windowId, int slot) {
            this.windowId = windowId;
            this.slot = slot;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            windowId = buf.readInt();
            slot = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(windowId);
            buf.writeInt(slot);
        }

        public static class Handler implements IMessageHandler<ToggleStorageCoreMessage, IMessage> {
            @Override
            public IMessage onMessage(ToggleStorageCoreMessage message, MessageContext context) {
                if (!Loader.isModLoaded("appliedenergistics2")) return null;
                EntityPlayerMP player = context.getServerHandler().player;
                player.getServerWorld().addScheduledTask(() -> {
                    if (player.openContainer == null || player.openContainer.windowId != message.windowId
                            || message.slot < 0 || message.slot >= player.openContainer.inventorySlots.size()) return;
                    Slot slot = player.openContainer.inventorySlots.get(message.slot);
                    ItemStack stack = slot.getStack();
                    if (stack.getItem() == ItemChainStorageTerminal.INSTANCE) {
                        ItemChainStorageTerminal.setEnabled(player, stack, !ItemChainStorageTerminal.isEnabled(stack));
                        slot.onSlotChanged();
                    }
                });
                return null;
            }
        }
    }

    public static class PreviewMessage implements IMessage {
        int totalCount;
        long[] pos;
        int neighborRangeLimit;

        public PreviewMessage() {
            neighborRangeLimit = 1;
        }

        public PreviewMessage(int totalCount, long[] pos, int neighborRangeLimit) {
            this.totalCount = totalCount;
            this.pos = pos;
            this.neighborRangeLimit = neighborRangeLimit;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            totalCount = buf.readInt();
            neighborRangeLimit = buf.readInt();
            int n = buf.readInt();
            if (totalCount < 0 || totalCount > 8192 || n < 0 || n > totalCount || buf.readableBytes() < n * 8) {
                throw new IllegalArgumentException("Invalid chain preview payload size");
            }
            pos = new long[n];
            for (int i = 0; i < n; i++) {
                pos[i] = buf.readLong();
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(totalCount);
            buf.writeInt(neighborRangeLimit);
            buf.writeInt(pos.length);
            for (long p : pos) {
                buf.writeLong(p);
            }
        }

        public static class Handler implements IMessageHandler<PreviewMessage, IMessage> {
            @Override
            public IMessage onMessage(PreviewMessage message, MessageContext ctx) {
                FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {

                    List<BlockPos> blocks = new ArrayList<>(message.pos.length);
                    for (long p : message.pos) {
                        blocks.add(BlockPos.fromLong(p));
                    }
                    ChainMiningStateManager.setPreviewBlocks(blocks);
                    ChainMiningStateManager.setPreviewTotal(message.totalCount);
                    ChainMiningStateManager.setServerNeighborRangeLimit(message.neighborRangeLimit);
                    int rendered = ChainMiningConfig.CLIENT.chainMiningEnablePreview
                            ? Math.min(blocks.size(), ChainMiningConfig.effectivePreviewRenderLimit()) : 0;
                    ChainMiningStateManager.setPreviewRendered(rendered);
                    ChainMiningStateManager.setPreviewHidden(ChainMiningConfig.CLIENT.chainMiningEnablePreview
                            ? Math.max(0, message.totalCount - rendered) : 0);
                });
                return null;
            }
        }
    }

    public static class StateMessage implements IMessage {
        boolean enabled;
        String shape = "SHAPELESS";
        String matchMode = "meta";
        int neighborRange = 1;
        int hitFaceOrdinal = -1;
        boolean hasTarget;
        long originPos;
        int previewRenderLimit = 128;
        boolean resyncPreview;

        public StateMessage() {

        }

        public StateMessage(boolean enabled, String shape, String matchMode, int neighborRange, int hitFaceOrdinal, boolean hasTarget, long originPos, int previewRenderLimit, boolean resyncPreview) {
            this.enabled = enabled;
            this.shape = shape;
            this.matchMode = matchMode;
            this.neighborRange = neighborRange;
            this.hitFaceOrdinal = hitFaceOrdinal;
            this.hasTarget = hasTarget;
            this.originPos = originPos;
            this.previewRenderLimit = previewRenderLimit;
            this.resyncPreview = resyncPreview;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            enabled = buf.readBoolean();
            shape = ByteBufUtils.readUTF8String(buf);
            matchMode = ByteBufUtils.readUTF8String(buf);
            neighborRange = buf.readInt();
            hitFaceOrdinal = buf.readInt();
            hasTarget = buf.readBoolean();
            originPos = buf.readLong();
            previewRenderLimit = buf.readInt();
            resyncPreview = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeBoolean(enabled);
            ByteBufUtils.writeUTF8String(buf, shape);
            ByteBufUtils.writeUTF8String(buf, matchMode);
            buf.writeInt(neighborRange);
            buf.writeInt(hitFaceOrdinal);
            buf.writeBoolean(hasTarget);
            buf.writeLong(originPos);
            buf.writeInt(previewRenderLimit);
            buf.writeBoolean(resyncPreview);
        }

        public static class Handler implements IMessageHandler<StateMessage, IMessage> {
            @Override
            public IMessage onMessage(StateMessage message, MessageContext context) {
                EntityPlayerMP playerMP = context.getServerHandler().player;

                playerMP.getServerWorld().addScheduledTask(() -> {
                    ChainMiningStateManager.PlayerState playerState = ChainMiningStateManager.getServerState(playerMP.getUniqueID());
                    if (message.resyncPreview || playerState.enabled != message.enabled) playerState.previewSync.invalidate();
                    playerState.enabled = message.enabled;
                    playerState.shape = message.shape;
                    playerState.matchMode = message.matchMode;
                    int limit = github.starfall063.chainmining.ChainMiningConfig.effectiveNeighborRange();
                    playerState.neighborRange = Math.max(1, Math.min(message.neighborRange, limit));
                    playerState.hitFace = (message.hitFaceOrdinal < 0 || message.hitFaceOrdinal >= EnumFacing.values().length) ? null : EnumFacing.values()[message.hitFaceOrdinal];
                    playerState.hasTarget = message.hasTarget;
                    playerState.originPos = message.originPos;
                    playerState.previewRenderLimit = Math.max(0, Math.min(message.previewRenderLimit, 8192));
                    playerState.dirty = true;
                });
                return null;
            }
        }
    }
}
