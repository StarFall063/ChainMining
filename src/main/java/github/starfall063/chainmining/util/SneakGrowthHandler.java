package github.starfall063.chainmining.util;

import github.starfall063.chainmining.ChainMiningConfig;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side growth pass for players who are sneaking. */
public final class SneakGrowthHandler {
    private static final Map<UUID, Boolean> PREVIOUS_SNEAKING = new ConcurrentHashMap<>();

    private SneakGrowthHandler() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        EntityPlayer player = event.player;
        if (player.world == null || player.world.isRemote) return;

        UUID playerId = player.getUniqueID();
        if (!ChainMiningConfig.SERVER.chainMiningSneakGrowEnabled) {
            PREVIOUS_SNEAKING.remove(playerId);
            return;
        }

        boolean sneaking = player.isSneaking();
        boolean previousSneaking = PREVIOUS_SNEAKING.getOrDefault(playerId, false);
        if (shouldRun(false, previousSneaking, sneaking)) {
            growNearby(player);
        }
        PREVIOUS_SNEAKING.put(playerId, sneaking);
    }

    public static void clearPlayer(UUID playerId) {
        PREVIOUS_SNEAKING.remove(playerId);
    }

    static boolean shouldRun(boolean worldRemote, boolean previousSneaking, boolean sneaking) {
        return !worldRemote && !previousSneaking && sneaking;
    }

    private static void growNearby(EntityPlayer player) {
        if (player.world.isRemote) return;
        int radius = ChainMiningConfig.effectiveSneakGrowRadius();
        BlockPos center = player.getPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (!player.world.isBlockLoaded(pos)) continue;

                    net.minecraft.block.state.IBlockState state = player.world.getBlockState(pos);
                    if (!(state.getBlock() instanceof IGrowable)) continue;

                    String registryName = state.getBlock().getRegistryName() == null
                            ? null : state.getBlock().getRegistryName().toString();
                    int meta = state.getBlock().getMetaFromState(state);
                    if (!SneakGrowthFilter.isAllowed(registryName, meta,
                            ChainMiningConfig.SERVER.chainMiningSneakGrowWhitelist,
                            ChainMiningConfig.SERVER.chainMiningSneakGrowBlacklist)) continue;

                    IGrowable growable = (IGrowable) state.getBlock();
                    if (!growable.canGrow(player.world, pos, state, false)) continue;

                    BonemealEvent bonemeal = new BonemealEvent(player, player.world, pos, state,
                            EnumHand.MAIN_HAND, new ItemStack(Items.DYE, 1, 15));
                    if (MinecraftForge.EVENT_BUS.post(bonemeal)) continue;
                    if (bonemeal.getResult() == net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW) continue;

                    net.minecraft.block.state.IBlockState currentState = player.world.getBlockState(pos);
                    if (!(currentState.getBlock() instanceof IGrowable)) continue;
                    IGrowable currentGrowable = (IGrowable) currentState.getBlock();
                    if (!currentGrowable.canGrow(player.world, pos, currentState, false)) continue;

                    currentGrowable.grow(player.world, player.world.rand, pos, currentState);
                    // Match vanilla bone meal feedback after the growth attempt.
                    player.world.playEvent(2005, pos, 0);
                }
            }
        }
    }
}
