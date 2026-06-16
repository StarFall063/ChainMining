package github.starfall063.chainmining;

import github.starfall063.chainmining.network.ChainMiningNetwork;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChainMiningEventHandler {
    private static final double DROP_COLLECTION_RANGE_SQ = 1024.0D;

    private final Map<UUID, MiningSession> activeSessions = new ConcurrentHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
            return;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;

        if (this.activeSessions.containsKey(serverPlayer.getUniqueID())) {
            return;
        }

        ChainMiningStateManager.PlayerState state = ChainMiningStateManager.getServerState(serverPlayer);
        if (!state.isEnabled() || !ChainMiningHooks.hasEnoughFoodToChain(serverPlayer)) {
            return;
        }

        EnumFacing face = ChainMiningHooks.resolveHitFace(serverPlayer, event.getPos());
        List<BlockPos> targets = ChainMiningHooks.collectMiningSelection(
                serverPlayer.world,
                serverPlayer,
                event.getPos(),
                face,
                state.getShapeMode(),
                state.getMatchMode(),
                ChainMiningConfig.maxBlocks,
                state.getNeighborRange(),
                ChainMiningConfig.directionalRange
        );
        if (targets.size() <= 1) {
            return;
        }

        MiningSession session = new MiningSession(serverPlayer.world, event.getPos(), serverPlayer.getUniqueID());
        this.activeSessions.put(serverPlayer.getUniqueID(), session);
        try {
            for (BlockPos target : targets) {
                if (event.getPos().equals(target)) {
                    continue;
                }
                if (!ChainMiningHooks.isWithinReach(serverPlayer, target)) {
                    continue;
                }
                if (!serverPlayer.capabilities.isCreativeMode && serverPlayer.getHeldItemMainhand().isEmpty()
                        && !ChainMiningStateManager.getEffectiveIgnoreHeldItem()) {
                    break;
                }
                if (!ChainMiningHooks.canBreakSecuredBlock(serverPlayer.world, target, serverPlayer)) {
                    continue;
                }
                if (!serverPlayer.interactionManager.tryHarvestBlock(target)) {
                    continue;
                }
                ChainMiningHooks.applyChainExhaustion(serverPlayer);
                if (!ChainMiningHooks.hasEnoughFoodToChain(serverPlayer)) {
                    break;
                }
            }
        } finally {
            MiningSession completedSession = this.activeSessions.remove(serverPlayer.getUniqueID());
            flushMiningSession(completedSession);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || this.activeSessions.isEmpty()) {
            return;
        }

        Entity entity = event.getEntity();
        for (MiningSession session : this.activeSessions.values()) {
            if (entity.world != session.world) {
                continue;
            }
            if (entity.getDistanceSq(session.dropPos.getX() + 0.5D, session.dropPos.getY() + 0.5D, session.dropPos.getZ() + 0.5D) > DROP_COLLECTION_RANGE_SQ) {
                continue;
            }
            if (entity instanceof EntityItem) {
                session.items.add(((EntityItem) entity).getItem().copy());
                event.setCanceled(true);
                return;
            } else if (entity instanceof EntityXPOrb) {
                session.experience += ((EntityXPOrb) entity).getXpValue();
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        if (!(player instanceof EntityPlayerMP) || player instanceof FakePlayer) {
            return;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;

        ItemStack held = serverPlayer.getHeldItemMainhand();

        ChainMiningStateManager.PlayerState state = ChainMiningStateManager.getServerState(serverPlayer);
        if (!state.isEnabled() || !ChainMiningHooks.hasEnoughFoodToChain(serverPlayer)) {
            return;
        }

        BlockPos pos = event.getPos();
        EnumFacing face = event.getFace() == null ? EnumFacing.UP : event.getFace();
        ChainMiningHooks.SelectionTarget target = ChainMiningHooks.resolveRightClickTarget(
                serverPlayer.world,
                serverPlayer,
                pos,
                face,
                held,
                state.getShapeMode(),
                state.getMatchMode(),
                ChainMiningConfig.maxBlocks,
                state.getNeighborRange(),
                ChainMiningConfig.directionalRange
        );
        if (target == null || target.getPositions().isEmpty()) {
            return;
        }

        if (ChainMiningHooks.applyRightClickAction(serverPlayer, serverPlayer.world, target, face, held)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        syncChainMiningConfig(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncChainMiningConfig(event.player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncChainMiningConfig(event.player);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.player.getUniqueID();
        MiningSession orphanedSession = this.activeSessions.remove(playerId);
        flushMiningSession(orphanedSession);
        ChainMiningStateManager.clearServerState(playerId);
    }

    private static void syncChainMiningConfig(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ChainMiningNetwork.sendConfigToPlayer((EntityPlayerMP) player,
                    ChainMiningConfig.maxBlocks,
                    ChainMiningConfig.previewRenderLimit,
                    ChainMiningConfig.directionalRange,
                    ChainMiningConfig.ignoreHeldItem);
        }
    }

    private static void flushMiningSession(MiningSession session) {
        if (session == null) {
            return;
        }

        for (ItemStack stack : session.items) {
            if (!stack.isEmpty()) {
                Block.spawnAsEntity(session.world, session.dropPos, stack);
            }
        }

        if (session.experience > 0) {
            session.world.spawnEntity(new EntityXPOrb(
                    session.world,
                    session.dropPos.getX() + 0.5D,
                    session.dropPos.getY() + 0.5D,
                    session.dropPos.getZ() + 0.5D,
                    session.experience
            ));
        }
    }

    private static final class MiningSession {
        private final World world;
        private final BlockPos dropPos;
        private final UUID ownerId;
        private final List<ItemStack> items = new java.util.ArrayList<>();
        private int experience;

        private MiningSession(World world, BlockPos dropPos, UUID ownerId) {
            this.world = world;
            this.dropPos = dropPos;
            this.ownerId = ownerId;
        }
    }
}
