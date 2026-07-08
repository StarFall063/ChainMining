package github.starfall063.chainmining.network;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.util.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

import java.util.List;
import java.util.UUID;

public final class ChainMiningNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    private ChainMiningNetwork() {

    }

    public static class ChainMiningPacket implements IMessage {
        private BlockPos startPos;
        private String matchMode;
        private UUID playerUUID;

        public ChainMiningPacket() {

        }

        public ChainMiningPacket(BlockPos startPos, BlockMatchMode matchMode, UUID playerUUID) {
            this.startPos = startPos;
            this.matchMode = matchMode.getSerializedName();
            this.playerUUID = playerUUID;
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            startPos = BlockPos.fromLong(buf.readLong());
            matchMode = ByteBufUtils.readUTF8String(buf);
            playerUUID = new UUID(buf.readLong(), buf.readLong());
        }

        @Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(startPos.toLong());
            ByteBufUtils.writeUTF8String(buf, matchMode);
            buf.writeLong(playerUUID.getMostSignificantBits());
            buf.writeLong(playerUUID.getLeastSignificantBits());
        }
    }

    public static class Handler implements IMessageHandler<ChainMiningPacket, IMessage> {
        @Override
        public IMessage onMessage(ChainMiningPacket message, MessageContext context) {
            EntityPlayerMP playerMP = context.getServerHandler().player;
            World world = playerMP.world;
            BlockPos pos = message.startPos;
            BlockMatchMode mode = BlockMatchMode.fromName(message.matchMode);

            playerMP.getServerWorld().addScheduledTask(() -> {
                IBlockState state = world.getBlockState(pos);
                if (state.getBlock().isAir(state, world, pos)) return;

                BlockIdentity sourceId = BlockIdentity.from(world, pos, state, mode);
                if (sourceId == null) return;

                ItemStack tool = playerMP.getHeldItemMainhand();
                if (!ChainMiningConfig.SERVER.chainMiningIgnoreHeldItem) {
                    if (ChainMiningHooks.isToolBlacklisted(tool)) return;
                }
                if (ChainMiningHooks.isBlockBlacklisted(world, pos, state)) return;
                if (!playerMP.capabilities.isCreativeMode) {
                    if (playerMP.getFoodStats().getFoodLevel() < ChainMiningConfig.SERVER.chainMiningMinFoodLevel) return;
                }

                ChainShapeMode shapeMode = ChainShapeMode.fromName(ChainMiningConfig.CLIENT.chainMiningShape);
                EnumFacing hitFace = ChainMiningStateManager.getHitFace();
                List<BlockPos> blocks = ChainMiningHooks.scanBlocks(
                        world, pos, state, sourceId, mode,
                        ChainMiningConfig.SERVER.chainMiningMaxBlocks,
                        ChainMiningConfig.SERVER.chainMiningNeighborRange,
                        playerMP,
                        shapeMode,
                        hitFace
                );
                ChainMiningHooks.executeChainMining(playerMP, blocks, tool);
            });
            return null;
        }
    }
}
