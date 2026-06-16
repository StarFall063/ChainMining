package github.starfall063.chainmining.network;

import github.starfall063.chainmining.BlockMatchMode;
import github.starfall063.chainmining.ChainMining;
import github.starfall063.chainmining.ChainMiningStateManager;
import github.starfall063.chainmining.ChainShapeMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;

public final class ChainMiningNetwork {
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(ChainMining.MOD_ID + "_cm");
    private static boolean initialized;

    private ChainMiningNetwork() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        CHANNEL.registerMessage(StateMessageHandler.class, StateMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(ConfigMessageHandler.class, ConfigMessage.class, 1, Side.CLIENT);
    }

    public static void sendStateToServer(boolean enabled, ChainShapeMode shapeMode, BlockMatchMode matchMode, int neighborRange) {
        CHANNEL.sendToServer(new StateMessage(enabled, shapeMode, matchMode, neighborRange));
    }

    public static void sendConfigToPlayer(EntityPlayerMP player, int maxBlocks, int previewRenderLimit, int directionalRange, boolean ignoreHeldItem) {
        if (player != null) {
            CHANNEL.sendTo(new ConfigMessage(maxBlocks, previewRenderLimit, directionalRange, ignoreHeldItem), player);
        }
    }

    public static final class StateMessage implements IMessage {
        private boolean enabled;
        private String shapeName;
        private String matchModeName;
        private int neighborRange;

        public StateMessage() {
        }

        public StateMessage(boolean enabled, ChainShapeMode shapeMode, BlockMatchMode matchMode, int neighborRange) {
            this.enabled = enabled;
            this.shapeName = (shapeMode == null ? ChainShapeMode.SHAPELESS : shapeMode).getSerializedName();
            this.matchModeName = (matchMode == null ? BlockMatchMode.META_ONLY : matchMode).getSerializedName();
            this.neighborRange = neighborRange;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.enabled = buf.readBoolean();
            this.shapeName = ByteBufUtils.readUTF8String(buf);
            this.matchModeName = ByteBufUtils.readUTF8String(buf);
            this.neighborRange = buf.readInt();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeBoolean(this.enabled);
            ByteBufUtils.writeUTF8String(buf, this.shapeName == null ? ChainShapeMode.SHAPELESS.getSerializedName() : this.shapeName);
            ByteBufUtils.writeUTF8String(buf, this.matchModeName == null ? BlockMatchMode.META_ONLY.getSerializedName() : this.matchModeName);
            buf.writeInt(neighborRange);
        }
    }

    public static final class ConfigMessage implements IMessage {
        private int maxBlocks;
        private int previewRenderLimit;
        private int directionalRange;
        private boolean ignoreHeldItem;

        public ConfigMessage() {
        }

        public ConfigMessage(int maxBlocks, int previewRenderLimit, int directionalRange, boolean ignoreHeldItem) {
            this.maxBlocks = maxBlocks;
            this.previewRenderLimit = previewRenderLimit;
            this.directionalRange = directionalRange;
            this.ignoreHeldItem = ignoreHeldItem;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            this.maxBlocks = buf.readInt();
            this.previewRenderLimit = buf.readInt();
            this.directionalRange = buf.readInt();
            this.ignoreHeldItem = buf.readBoolean();
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeInt(this.maxBlocks);
            buf.writeInt(this.previewRenderLimit);
            buf.writeInt(this.directionalRange);
            buf.writeBoolean(this.ignoreHeldItem);
        }
    }

    public static final class StateMessageHandler implements IMessageHandler<StateMessage, IMessage> {
        @Override
        public IMessage onMessage(StateMessage message, MessageContext ctx) {
            if (ctx.getServerHandler() == null || ctx.getServerHandler().player == null) {
                return null;
            }
            EntityPlayerMP player = ctx.getServerHandler().player;
            UUID playerId = player.getUniqueID();

            if (!ChainMiningStateManager.isStateUpdateAllowed(playerId)) {
                return null;
            }

            player.getServerWorld().addScheduledTask(() ->
                    ChainMiningStateManager.updateServerState(
                            playerId,
                            message.enabled,
                            ChainShapeMode.fromName(message.shapeName),
                            BlockMatchMode.fromName(message.matchModeName),
                            message.neighborRange
                    )
            );
            return null;
        }
    }

    public static final class ConfigMessageHandler implements IMessageHandler<ConfigMessage, IMessage> {
        @Override
        public IMessage onMessage(ConfigMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> ChainMiningStateManager.applySyncedConfig(message.maxBlocks, message.previewRenderLimit, message.directionalRange, message.ignoreHeldItem));
            return null;
        }
    }
}
