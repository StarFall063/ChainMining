package github.starfall063.chainmining.server;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.network.ChainMiningNetwork;
import github.starfall063.chainmining.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Collections;
import java.util.UUID;

public final class ChainMiningServerPreview {
    private ChainMiningServerPreview() {

    }

    public static void recompute(EntityPlayerMP player) {
        UUID uuid = player.getUniqueID();
        ChainMiningStateManager.PlayerState state = ChainMiningStateManager.getServerState(uuid);
        if (!state.enabled || !state.hasTarget) {
            send(player, state, Collections.emptyList());
            return;
        }

        World world = player.world;
        BlockPos origin = BlockPos.fromLong(state.originPos);
        if (!world.isBlockLoaded(origin)) {
            send(player, state, Collections.emptyList());
            return;
        }
        IBlockState sState = world.getBlockState(origin);
        if (!player.capabilities.isCreativeMode) {
            if (ChainMiningHooks.isToolBlacklisted(player.getHeldItemMainhand())
                    || ChainMiningHooks.isBlockBlacklisted(world, origin, sState)
                    || !ChainMiningHooks.canChainMineBlock(world, origin, sState, player, player.getHeldItemMainhand())
                    || !ChainMiningHooks.hasChainMiningAbility(player.getHeldItemMainhand())) {
                send(player, state, Collections.emptyList());
                return;
            }
        }
        if (sState.getBlock().isAir(sState, world, origin)) {
            send(player, state, Collections.emptyList());
            return;
        }

        BlockMatchMode mode = BlockMatchMode.fromName(state.matchMode);
        ChainShapeMode shape = ChainShapeMode.fromName(state.shape);
        BlockIdentity id = BlockIdentity.from(world, origin, sState, mode);
        if (id == null) {
            send(player, state, Collections.emptyList());
            return;
        }

        List<BlockPos> blockPos = ChainMiningHooks.scanBlocks(world, origin, sState, id, mode, ChainMiningConfig.effectiveMaxBlocks(), Math.min(state.neighborRange, ChainMiningConfig.effectiveNeighborRange()), player, shape, state.hitFace);
        send(player, state, blockPos);
    }

    private static void send(EntityPlayerMP player, ChainMiningStateManager.PlayerState state, List<BlockPos> blocks) {
        int rangeLimit = ChainMiningConfig.effectiveNeighborRange();
        if (!state.previewSync.update(blocks, rangeLimit, state.previewRenderLimit)) return;
        ChainMiningNetwork.CHANNEL.sendTo(new ChainMiningNetwork.PreviewMessage(
                state.previewSync.getTotal(), state.previewSync.getPositions(), rangeLimit), player);
    }
}
