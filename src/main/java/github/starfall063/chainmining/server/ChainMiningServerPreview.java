package github.starfall063.chainmining.server;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.network.ChainMiningNetwork;
import github.starfall063.chainmining.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public final class ChainMiningServerPreview {
    private ChainMiningServerPreview() {

    }

    public static void recompute(EntityPlayerMP player) {
        UUID uuid = player.getUniqueID();
        ChainMiningStateManager.PlayerState state = ChainMiningStateManager.getServerState(uuid);
        if (!state.enabled || !state.hasTarget) {
            send(player, 0, new long[0]);
            return;
        }

        World world = player.world;
        BlockPos origin = BlockPos.fromLong(state.originPos);
        IBlockState sState = world.getBlockState(origin);
        if (sState.getBlock().isAir(sState, world, origin) || ChainMiningHooks.isToolBlacklisted(player.getHeldItemMainhand()) || ChainMiningHooks.isBlockBlacklisted(world, origin, sState) || !ChainMiningHooks.canChainMineBlock(world, origin, sState, player, player.getHeldItemMainhand())) {
            send(player, 0, new long[0]);
            return;
        }

        BlockMatchMode mode = BlockMatchMode.fromName(state.matchMode);
        ChainShapeMode shape = ChainShapeMode.fromName(state.shape);
        BlockIdentity id = BlockIdentity.from(world, origin, sState, mode);
        if (id == null) {
            send(player, 0, new long[0]);
            return;
        }

        List<BlockPos> blockPos = ChainMiningHooks.scanBlocks(world, origin, sState, id, mode, ChainMiningConfig.SERVER.chainMiningMaxBlocks, state.neighborRange, player, shape, state.hitFace);
        int total = blockPos.size();
        long[] array = new long[total];
        for (int i = 0; i < total; i++) array[i] = blockPos.get(i).toLong();
        send(player, total, array);
    }

    private static void send(EntityPlayerMP player, int total, long[] pos) {
        ChainMiningNetwork.CHANNEL.sendTo(new ChainMiningNetwork.PreviewMessage(total, pos), player);
    }
}
