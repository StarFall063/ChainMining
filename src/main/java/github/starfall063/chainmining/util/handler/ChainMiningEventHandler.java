package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class ChainMiningEventHandler {
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;
        if (!(event.getPlayer() instanceof EntityPlayerMP playerMP)) return;
        UUID uuid = playerMP.getUniqueID();

        if (ACTIVE.contains(uuid)) return;
        if (!ChainMiningStateManager.isServerEnabled(uuid)) return;

        ChainMiningStateManager.PlayerState playerState = ChainMiningStateManager.getServerState(uuid);
        BlockPos pos = event.getPos();
        IBlockState state = event.getState();
        ItemStack tool = playerMP.getHeldItemMainhand();

        if (!playerMP.capabilities.isCreativeMode) {
            if (ChainMiningHooks.isToolBlacklisted(tool)) return;
            if (ChainMiningHooks.isBlockBlacklisted(playerMP.world, pos, state)) return;
            if (!ChainMiningHooks.canChainMineBlock(playerMP.world, pos, state, playerMP, tool)) return;
            if (playerMP.getFoodStats().getFoodLevel() < ChainMiningConfig.SERVER.chainMiningMinFoodLevel) return;
            if (!ChainMiningHooks.hasChainMiningAbility(tool)) return;
        }

        BlockMatchMode mode = BlockMatchMode.fromName(playerState.matchMode);
        ChainShapeMode shapeMode = ChainShapeMode.fromName(playerState.shape);
        BlockIdentity sourceId = BlockIdentity.from(playerMP.world, pos, state, mode);
        if (sourceId == null) return;

        List<BlockPos> blocks = ChainMiningHooks.scanBlocks(playerMP.world, pos, state, sourceId, mode, ChainMiningConfig.SERVER.chainMiningMaxBlocks, playerState.neighborRange, playerMP, shapeMode, playerState.hitFace);

        if (blocks.size() <= 1) return;

        event.setCanceled(true);
        ACTIVE.add(uuid);
        Session s = new Session(pos, new HashSet<>(blocks));
        SESSIONS.put(uuid, s);
        try {
            ChainMiningHooks.executeChainMining(playerMP, blocks, tool);
        } finally {
            SESSIONS.remove(uuid);
            ACTIVE.remove(uuid);
            flushSession(playerMP.world, s);
            playerState.dirty = true;
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityItem) && !(entity instanceof EntityXPOrb)) return;

        BlockPos entityPos = new BlockPos(entity.posX, entity.posY, entity.posZ);
        for (Session s : SESSIONS.values()) {
            if (s.area.contains(entityPos)) {
                if (entity instanceof EntityItem) {
                    s.drops.add(((EntityItem) entity).getItem().copy());
                } else {
                    s.xp += ((EntityXPOrb) entity).xpValue;
                }
                event.setCanceled(true);
                return;
            }
        }
    }

    private static void flushSession(World world, Session session) {
        Map<ChainMiningHooks.ItemKey, Integer> merged = new LinkedHashMap<>();
        for (ItemStack stack : session.drops) {
            if (stack.isEmpty()) continue;
            merged.merge(new ChainMiningHooks.ItemKey(stack), stack.getCount(), Integer::sum);
        }

        for (Map.Entry<ChainMiningHooks.ItemKey, Integer> e : merged.entrySet()) {
            int total = e.getValue();
            ItemStack base = e.getKey().toStack();
            while (total > 0) {
                int size = Math.min(total, base.getMaxStackSize());
                ItemStack out = base.copy();
                out.setCount(size);
                Block.spawnAsEntity(world, session.origin, out);
                total -= size;
            }
        }

        if (session.xp > 0) {
            world.spawnEntity(new EntityXPOrb(world, session.origin.getX()+0.5, session.origin.getY()+0.5, session.origin.getZ()+0.5, session.xp));
        }
    }
}


