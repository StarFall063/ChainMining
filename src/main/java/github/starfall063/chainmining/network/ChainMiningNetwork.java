package github.starfall063.chainmining.network;

import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.server.ChainMiningServerPreview;
import github.starfall063.chainmining.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
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

    public static class PreviewMessage implements IMessage {
        int totalCount;
        long[] pos;

        public PreviewMessage() {

        }
        public PreviewMessage(int totalCount, long[] pos) {
            this.totalCount = totalCount;
            this.pos = pos;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            totalCount = buf.readInt();
            int n = buf.readInt();
            pos = new long[n];
            for (int i = 0; i < n; i++) {
                pos[i] = buf.readLong();
            }
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(totalCount);
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
                   ChainMiningStateManager.setPreviewRendered(blocks.size());
                   ChainMiningStateManager.setPreviewHidden(Math.max(0, message.totalCount - blocks.size()));
                });
                return null;
            }
        }
    }

    public static class ConfigMessage implements IMessage {
        int maxBlocks;
        boolean ignoreHeldItem;
        String[] toolBlacklist = new String[0];
        String[] blockBlacklist = new String[0];

        public ConfigMessage() {

        }

        public ConfigMessage(int maxBlocks, boolean ignoreHeldItem, String[] toolBlacklist, String[] blockBlacklist) {
            this.maxBlocks = maxBlocks;
            this.ignoreHeldItem = ignoreHeldItem;
            this.toolBlacklist = toolBlacklist;
            this.blockBlacklist = blockBlacklist;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            maxBlocks = buf.readInt();
            ignoreHeldItem = buf.readBoolean();
            toolBlacklist = readArray(buf);
            blockBlacklist = readArray(buf);
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(maxBlocks);
            buf.writeBoolean(ignoreHeldItem);
            writeArray(buf, toolBlacklist);
            writeArray(buf, blockBlacklist);
        }

        private static void writeArray(ByteBuf buf, String[] array) {
            buf.writeInt(array.length);
            for (String s : array) {
                ByteBufUtils.writeUTF8String(buf, s);
            }
        }

        private static String[] readArray(ByteBuf buf) {
            int n = buf.readInt();
            String[] array = new String[n];
            for (int i = 0; i < n; i++) {
                array[i] = ByteBufUtils.readUTF8String(buf);
            }
            return array;
        }

        public static class Handler implements IMessageHandler<ConfigMessage, IMessage> {
            @Override
            public IMessage onMessage(ConfigMessage message, MessageContext context) {
                FMLCommonHandler.instance().getWorldThread(context.netHandler).addScheduledTask(() -> ChainMiningStateManager.applySyncedConfig(
                        message.maxBlocks,
                        message.ignoreHeldItem,
                        message.toolBlacklist,
                        message.blockBlacklist
                ));
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

        public StateMessage() {

        }

        public StateMessage(boolean enabled, String shape, String matchMode, int neighborRange, int hitFaceOrdinal, boolean hasTarget, long originPos) {
            this.enabled = enabled;
            this.shape = shape;
            this.matchMode = matchMode;
            this.neighborRange = neighborRange;
            this.hitFaceOrdinal = hitFaceOrdinal;
            this.hasTarget = hasTarget;
            this.originPos = originPos;
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
        }

        public static class Handler implements IMessageHandler<StateMessage, IMessage> {
            @Override
            public IMessage onMessage(StateMessage message, MessageContext context) {
                EntityPlayerMP playerMP = context.getServerHandler().player;

                playerMP.getServerWorld().addScheduledTask(() -> {
                    ChainMiningStateManager.PlayerState playerState = ChainMiningStateManager.getServerState(playerMP.getUniqueID());
                    playerState.enabled = message.enabled;
                    playerState.shape = message.shape;
                    playerState.matchMode = message.matchMode;
                    playerState.neighborRange = Math.max(1, Math.min(message.neighborRange, 5));
                    playerState.hitFace = (message.hitFaceOrdinal < 0 || message.hitFaceOrdinal >= EnumFacing.values().length) ? null : EnumFacing.values()[message.hitFaceOrdinal];
                    playerState.hasTarget = message.hasTarget;
                    playerState.originPos = message.originPos;
                    playerState.dirty = true;
                });
                return null;
            }
        }
    }
}
