package github.starfall063.chainmining;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;

import java.lang.reflect.Method;
import java.util.*;

public final class ChainMiningHooks {
    public static final int DEFAULT_PREVIEW_RENDER_LIMIT = 256;
    private static final Map<Integer, List<BlockPos>> OFFSET_CACHE = new HashMap<>();

    private ChainMiningHooks() {
    }

    public static boolean shouldBypassToolUse(EntityPlayer player, World world, BlockPos pos, EnumFacing face, EnumHand hand) {
        if (player == null || world == null || pos == null || face == null || hand != EnumHand.MAIN_HAND) {
            return false;
        }

        ItemStack held = player.getHeldItemMainhand();

        if (world.isRemote) {
            if (!ChainMiningStateManager.isClientEnabled()) {
                return false;
            }
        } else if (!ChainMiningStateManager.getServerState(player).isEnabled()) {
            return false;
        }

        return canTill(world, pos, face, held);
    }

    public static boolean canUseMiningChain(EntityPlayer player, ItemStack stack) {
        if (player == null) {
            return false;
        }
        if (player.capabilities.isCreativeMode) {
            return true;
        }
        if (ChainMiningStateManager.getEffectiveIgnoreHeldItem()) {
            return true;
        }
        return stack != null && !stack.isEmpty();
    }

    public static boolean canUseRightClickChain(ItemStack stack) {
        if (ChainMiningStateManager.getEffectiveIgnoreHeldItem()) {
            return stack == null || stack.isEmpty() || isTillingItem(stack) || isSowingItem(stack);
        }
        return stack != null && !stack.isEmpty() && (isTillingItem(stack) || isSowingItem(stack));
    }

    public static boolean canUseAnyChainAction(EntityPlayer player, ItemStack stack) {
        return canUseMiningChain(player, stack) || canUseRightClickChain(stack);
    }

    public static boolean hasEnoughFoodToChain(EntityPlayer player) {
        if (player == null || player.capabilities.isCreativeMode) {
            return true;
        }
        FoodStats foodStats = player.getFoodStats();
        return foodStats != null && foodStats.getFoodLevel() > ChainMiningConfig.minFoodLevel;
    }

    public static void applyChainExhaustion(EntityPlayer player) {
        if (player != null && !player.capabilities.isCreativeMode && ChainMiningConfig.exhaustionPerBlock > 0.0F) {
            player.addExhaustion(ChainMiningConfig.exhaustionPerBlock);
        }
    }

    public static EnumFacing resolveHitFace(EntityPlayer player, BlockPos targetPos) {
        if (player == null || targetPos == null) {
            return EnumFacing.UP;
        }

        RayTraceResult rayTrace = player.rayTrace(6.0D, 1.0F);
        if (rayTrace != null && rayTrace.typeOfHit == RayTraceResult.Type.BLOCK && targetPos.equals(rayTrace.getBlockPos()) && rayTrace.sideHit != null) {
            return rayTrace.sideHit;
        }

        return resolvePlayerFacing(player);
    }

    public static SelectionTarget resolveRightClickTarget(World world, EntityPlayer player, BlockPos origin, EnumFacing face, ItemStack held, ChainShapeMode shapeMode, BlockMatchMode matchMode, int maxBlocks, int neighborRange, int directionalRange) {
        if (world == null || player == null || origin == null || face == null || !canUseRightClickChain(held)) {
            return null;
        }

        if (shapeMode == ChainShapeMode.TUNNEL) {
            return null;
        }

        if (canHarvest(world, origin, held)) {
            return new SelectionTarget(ChainAction.HARVEST, collectSelection(world, player, origin, face, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, ChainAction.HARVEST));
        }
        if (canSow(world, origin, face, held)) {
            return new SelectionTarget(ChainAction.SOW, collectSelection(world, player, origin, face, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, ChainAction.SOW));
        }
        if (canTill(world, origin, face, held)) {
            return new SelectionTarget(ChainAction.TILL, collectSelection(world, player, origin, face, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, ChainAction.TILL));
        }
        return null;
    }

    public static SelectionTarget resolvePreviewTarget(World world, EntityPlayer player, BlockPos origin, EnumFacing face, ItemStack held, ChainShapeMode shapeMode, BlockMatchMode matchMode, int maxBlocks, int neighborRange, int directionalRange, int previewRenderLimit) {
        SelectionTarget rightClickTarget = resolvePreviewActionTarget(world, player, origin, face, held, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, previewRenderLimit);
        if (rightClickTarget != null && !rightClickTarget.positions.isEmpty()) {
            return rightClickTarget;
        }
        if (!canUseMiningChain(player, held)) {
            return null;
        }
        return collectPreviewSelectionTarget(world, player, origin, face, held, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, previewRenderLimit, ChainAction.MINE);
    }

    private static SelectionTarget resolvePreviewActionTarget(World world, EntityPlayer player, BlockPos origin, EnumFacing face, ItemStack held, ChainShapeMode shapeMode, BlockMatchMode matchMode, int maxBlocks, int neighborRange, int directionalRange, int previewRenderLimit) {
        if (world == null || player == null || origin == null || face == null || !canUseRightClickChain(held)) {
            return null;
        }

        if (canHarvest(world, origin, held)) {
            return collectPreviewSelectionTarget(world, player, origin, face, held, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, previewRenderLimit, ChainAction.HARVEST);
        }
        if (canSow(world, origin, face, held)) {
            return collectPreviewSelectionTarget(world, player, origin, face, held, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, previewRenderLimit, ChainAction.SOW);
        }
        if (canTill(world, origin, face, held)) {
            return collectPreviewSelectionTarget(world, player, origin, face, held, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, previewRenderLimit, ChainAction.TILL);
        }
        return null;
    }

    private static SelectionTarget collectPreviewSelectionTarget(World world, EntityPlayer player, BlockPos origin, EnumFacing face, ItemStack held, ChainShapeMode shapeMode, BlockMatchMode matchMode, int maxBlocks, int neighborRange, int directionalRange, int previewRenderLimit, ChainAction action) {
        PreviewSelectionResult result = collectPreviewSelection(world, player, origin, face, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, previewRenderLimit, action, held);
        if (result.positions.isEmpty()) {
            return null;
        }
        return new SelectionTarget(action, result.positions, result.totalCount, result.hiddenCount, result.countExact);
    }

    private static PreviewSelectionResult collectPreviewSelection(World world, EntityPlayer player, BlockPos origin, EnumFacing face, ChainShapeMode shapeMode, BlockMatchMode matchMode, int maxBlocks, int neighborRange, int directionalRange, int previewRenderLimit, ChainAction action, ItemStack held) {
        if (maxBlocks <= 0) {
            return PreviewSelectionResult.empty(action);
        }

        ChainMiningSelectionContext context = ChainMiningSelectionContext.create(world, player, origin, face, held, shapeMode, matchMode, action);
        if (context == null) {
            return PreviewSelectionResult.empty(action);
        }

        ChainMiningPreviewBudget previewBudget = ChainMiningPreviewBudget.create(previewRenderLimit, maxBlocks);
        int effectivePreviewRenderLimit = previewBudget.getRenderLimit();
        switch (context.getShapeMode()) {
            case PLANE:
                return PreviewSelectionResult.exact(action, collectDirectional(context.getWorld(), context.getPlayer(), context.getOrigin(), context.getOriginState(), context.getIdentity(), action, context.getMatchMode(), maxBlocks, directionalRange, context.getHeld()), effectivePreviewRenderLimit);
            case TUNNEL:
                return PreviewSelectionResult.exact(action, collectTunnel(context.getWorld(), context.getPlayer(), context.getOrigin(), context.getOriginState(), context.getIdentity(), action, context.getMatchMode(), maxBlocks, context.getHeld()), effectivePreviewRenderLimit);
            case SHAPELESS:
            default:
                return collectShapelessPreview(context.getWorld(), context.getPlayer(), context.getOrigin(), context.getFace(), context.getOriginState(), context.getIdentity(), action, context.getMatchMode(), maxBlocks, neighborRange, context.getHeld(), previewBudget);
        }
    }

    public static List<BlockPos> collectMiningSelection(World world, EntityPlayer player, BlockPos origin, EnumFacing face, ChainShapeMode shapeMode, BlockMatchMode matchMode, int maxBlocks, int neighborRange, int directionalRange) {
        return collectSelection(world, player, origin, face, shapeMode, matchMode, maxBlocks, neighborRange, directionalRange, ChainAction.MINE);
    }

    public static List<BlockPos> collectSelection(World world, EntityPlayer player, BlockPos origin, EnumFacing face, ChainShapeMode shapeMode, BlockMatchMode matchMode, int maxBlocks, int neighborRange, int directionalRange, ChainAction action) {
        if (maxBlocks <= 0) {
            return Collections.emptyList();
        }

        ChainMiningSelectionContext context = ChainMiningSelectionContext.create(world, player, origin, face, player == null ? ItemStack.EMPTY : player.getHeldItemMainhand(), shapeMode, matchMode, action);
        if (context == null) {
            return Collections.emptyList();
        }

        switch (context.getShapeMode()) {
            case PLANE:
                return collectDirectional(context.getWorld(), context.getPlayer(), context.getOrigin(), context.getOriginState(), context.getIdentity(), action, context.getMatchMode(), maxBlocks, directionalRange, context.getHeld());
            case TUNNEL:
                return collectTunnel(context.getWorld(), context.getPlayer(), context.getOrigin(), context.getOriginState(), context.getIdentity(), action, context.getMatchMode(), maxBlocks, context.getHeld());
            case SHAPELESS:
            default:
                return collectShapeless(context.getWorld(), context.getPlayer(), context.getOrigin(), context.getFace(), context.getOriginState(), context.getIdentity(), action, context.getMatchMode(), maxBlocks, neighborRange, context.getHeld());
        }
    }

    public static boolean applyRightClickAction(EntityPlayerMP player, World world, SelectionTarget target, EnumFacing face, ItemStack held) {
        if (player == null || world == null || target == null || target.positions.isEmpty()) {
            return false;
        }

        if (!hasEnoughFoodToChain(player)) {
            return false;
        }

        boolean changed = false;
        for (BlockPos pos : target.positions) {
            if (!isWithinReach(player, pos)) {
                continue;
            }
            if (target.action == ChainAction.TILL) {
                changed |= applyTill(player, world, pos, face, held);
            } else if (target.action == ChainAction.SOW) {
                changed |= applySow(player, world, pos, face, held);
            } else if (target.action == ChainAction.HARVEST) {
                changed |= applyHarvest(player, world, pos);
            }

            if (changed) {
                applyChainExhaustion(player);
                if (!hasEnoughFoodToChain(player)) {
                    break;
                }
            }

            if (!player.capabilities.isCreativeMode && held.isEmpty()) {
                break;
            }
        }
        return changed;
    }

    private static List<BlockPos> collectDirectional(World world, EntityPlayer player, BlockPos origin, IBlockState originState, BlockIdentity identity, ChainAction action, BlockMatchMode matchMode, int maxBlocks, int directionalRange, ItemStack held) {
        int lengthLimit = Math.min(maxBlocks, Math.max(1, directionalRange));
        List<BlockPos> result = new ArrayList<>(lengthLimit);
        EnumFacing direction = resolvePlayerFacing(player);

        for (int depth = 0; depth < lengthLimit; depth++) {
            BlockPos candidate = origin.offset(direction, depth);
            if (!matchesActionTarget(world, candidate, direction, player, held, originState, identity, action, matchMode)) {
                break;
            }
            result.add(candidate);
        }

        return result;
    }

    private static List<BlockPos> collectTunnelSlice(World world, EntityPlayer player, BlockPos origin, EnumFacing face, IBlockState originState, BlockIdentity identity, ChainAction action, BlockMatchMode matchMode, ItemStack held) {
        List<BlockPos> result = new ArrayList<>(9);
        EnumFacing.Axis axis = face.getAxis();

        for (int first = -1; first <= 1; first++) {
            for (int second = -1; second <= 1; second++) {
                BlockPos candidate;
                switch (axis) {
                    case X:
                        candidate = origin.add(0, first, second);
                        break;
                    case Y:
                        candidate = origin.add(first, 0, second);
                        break;
                    case Z:
                    default:
                        candidate = origin.add(first, second, 0);
                        break;
                }

                if (matchesActionTarget(world, candidate, face, player, held, originState, identity, action, matchMode)) {
                    result.add(candidate);
                }
            }
        }

        return sortByDistance(result, origin, Integer.MAX_VALUE);
    }

    private static List<BlockPos> collectTunnel(World world, EntityPlayer player, BlockPos origin, IBlockState originState, BlockIdentity identity, ChainAction action, BlockMatchMode matchMode, int maxBlocks, ItemStack held) {
        List<BlockPos> result = new ArrayList<>(Math.max(9, maxBlocks));
        Set<BlockPos> added = new HashSet<>(Math.max(16, Math.min(maxBlocks * 2, 2048)));
        EnumFacing direction = resolveTunnelDirection(player);

        for (int depth = 0; result.size() < maxBlocks; depth++) {
            BlockPos center = origin.offset(direction, depth);
            List<BlockPos> slice = collectTunnelSlice(world, player, center, direction, originState, identity, action, matchMode, held);
            if (slice.isEmpty()) {
                break;
            }
            for (BlockPos candidate : slice) {
                if (result.size() >= maxBlocks) {
                    break;
                }
                if (added.add(candidate)) {
                    result.add(candidate);
                }
            }
        }

        return result;
    }

    private static List<BlockPos> collectShapeless(World world, EntityPlayer player, BlockPos origin, EnumFacing face, IBlockState originState, BlockIdentity identity, ChainAction action, BlockMatchMode matchMode, int maxBlocks, int neighborRange, ItemStack held) {
        ShapelessBfsResult bfsResult = collectShapelessBfs(world, player, origin, face, originState, identity, action, matchMode, maxBlocks, neighborRange, held);
        return sortByDistance(bfsResult.positions, origin, maxBlocks);
    }

    private static PreviewSelectionResult collectShapelessPreview(World world, EntityPlayer player, BlockPos origin, EnumFacing face, IBlockState originState, BlockIdentity identity, ChainAction action, BlockMatchMode matchMode, int maxBlocks, int neighborRange, ItemStack held, ChainMiningPreviewBudget previewBudget) {
        int previewRenderLimit = previewBudget.getRenderLimit();
        int previewCollectionLimit = previewBudget.getCollectionLimit();
        ShapelessBfsResult bfsResult = collectShapelessBfs(world, player, origin, face, originState, identity, action, matchMode, previewCollectionLimit, neighborRange, held);
        List<BlockPos> ordered = sortByDistance(bfsResult.positions, origin, previewCollectionLimit);
        boolean previewProbeCapped = previewCollectionLimit < maxBlocks && bfsResult.wasTruncated;
        if (!previewProbeCapped) {
            return PreviewSelectionResult.exact(action, ordered, previewRenderLimit);
        }
        int renderedCount = Math.min(ordered.size(), previewRenderLimit);
        int knownHiddenCount = Math.max(1, ordered.size() - renderedCount);
        return PreviewSelectionResult.inexact(action, ordered, ordered.size(), knownHiddenCount);
    }

    static boolean isWithinReach(EntityPlayer player, BlockPos pos) {
        if (!ChainMiningConfig.reachFilter) {
            return true;
        }
        double reachDist = player.capabilities.isCreativeMode ? 5.0D : 4.5D;
        double reachSq = reachDist * reachDist + 4.0D;
        return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= reachSq;
    }

    private static ShapelessBfsResult collectShapelessBfs(World world, EntityPlayer player, BlockPos origin, EnumFacing face, IBlockState originState, BlockIdentity identity, ChainAction action, BlockMatchMode matchMode, int collectionLimit, int neighborRange, ItemStack held) {
        Set<BlockPos> visited = new HashSet<>(Math.max(64, Math.min(collectionLimit * 4, 8192)));
        List<BlockPos> result = new ArrayList<>(Math.min(collectionLimit, 256));
        Deque<BlockPos> pending = new ArrayDeque<>(Math.min(collectionLimit, 256));
        List<BlockPos> offsets = getNeighborOffsets(neighborRange);

        pending.add(origin);
        visited.add(origin);

        while (!pending.isEmpty() && result.size() < collectionLimit) {
            BlockPos current = pending.removeFirst();
            if (!matchesActionTarget(world, current, face, player, held, originState, identity, action, matchMode)) {
                continue;
            }
            result.add(current);
            for (BlockPos offset : offsets) {
                BlockPos next = current.add(offset);
                if (visited.add(next)) {
                    pending.addLast(next);
                }
            }
        }

        boolean truncated = result.size() >= collectionLimit && !pending.isEmpty();
        return new ShapelessBfsResult(result, truncated);
    }

    @Desugar
    private record ShapelessBfsResult(List<BlockPos> positions, boolean wasTruncated) {}


    private static List<BlockPos> getNeighborOffsets(int neighborRange) {
        int clampedRange = Math.max(1, neighborRange);
        Integer cacheKey = clampedRange;
        List<BlockPos> cached = OFFSET_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        int diameter = clampedRange * 2 + 1;
        List<BlockPos> offsets = new ArrayList<>(diameter * diameter * diameter - 1);
        for (int x = -clampedRange; x <= clampedRange; x++) {
            for (int y = -clampedRange; y <= clampedRange; y++) {
                for (int z = -clampedRange; z <= clampedRange; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    if (Math.max(Math.max(Math.abs(x), Math.abs(y)), Math.abs(z)) > clampedRange) {
                        continue;
                    }
                    offsets.add(new BlockPos(x, y, z));
                }
            }
        }
        offsets.sort(Comparator.comparingDouble(pos -> pos.distanceSq(0.0D, 0.0D, 0.0D)));
        List<BlockPos> immutable = Collections.unmodifiableList(offsets);
        OFFSET_CACHE.put(cacheKey, immutable);
        return immutable;
    }

    private static List<BlockPos> sortByDistance(List<BlockPos> positions, BlockPos origin, int maxBlocks) {
        positions.sort(Comparator.comparingDouble(pos -> pos.distanceSq(origin)));
        if (positions.size() > maxBlocks) {
            positions.subList(maxBlocks, positions.size()).clear();
        }
        return positions;
    }

    private static boolean matchesActionTarget(World world, BlockPos pos, EnumFacing face, EntityPlayer player, ItemStack held, IBlockState originState, BlockIdentity identity, ChainAction action, BlockMatchMode matchMode) {
        IBlockState state = world.getBlockState(pos);
        if (action == ChainAction.SOW) {
            return matchesSowBase(originState, state) && canActOn(world, pos, state, face, player, held, action);
        }
        return identity != null && identity.matches(world, pos, state, matchMode) && canActOn(world, pos, state, face, player, held, action);
    }

    static boolean canActOn(World world, BlockPos pos, IBlockState state, EnumFacing face, EntityPlayer player, ItemStack held, ChainAction action) {
        switch (action) {
            case TILL:
                return canTill(world, pos, face, held);
            case HARVEST:
                return canHarvest(world, pos, held);
            case SOW:
                return canSow(world, pos, face, held);
            case MINE:
            default:
                return canChainMineBlock(world, pos, state, player, held);
        }
    }

    private static boolean matchesSowBase(IBlockState originState, IBlockState candidateState) {
        return originState != null && candidateState != null && originState.getBlock() == candidateState.getBlock();
    }

    private static boolean canChainMineBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player, ItemStack held) {
        if (world == null || pos == null || state == null || state.getBlock() == Blocks.AIR) {
            return false;
        }
        if (player != null && player.capabilities.isCreativeMode) {
            return true;
        }
        if (ChainMiningStateManager.getEffectiveIgnoreHeldItem()) {
            if (isMiningTool(held)) {
                return held.getItem().canHarvestBlock(state, held);
            }
            return true;
        }
        if (state.getBlockHardness(world, pos) < 0.0F) {
            return false;
        }
        if (state.getMaterial().isToolNotRequired()) {
            return true;
        }
        return isMiningTool(held);
    }

    public static boolean canBreakSecuredBlock(World world, BlockPos pos, EntityPlayer player) {
        if (player == null || player.capabilities.isCreativeMode) {
            return true;
        }
        TileEntity te = world.getTileEntity(pos);
        if (te == null) {
            return true;
        }
        NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
        java.util.UUID playerId = player.getUniqueID();
        for (String ownerKey : new String[]{"owner", "Owner", "ownerUUID"}) {
            if (tag.hasKey(ownerKey, 8)) {
                String owner = tag.getString(ownerKey);
                if (!"none".equalsIgnoreCase(owner) && !owner.equals(playerId.toString())) {
                    if (tag.hasKey("securityMode", 8)) {
                        String mode = tag.getString("securityMode");
                        if (!"public".equalsIgnoreCase(mode) && !"trusted".equalsIgnoreCase(mode)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isTillingItem(ItemStack held) {
        if (held.isEmpty()) {
            return false;
        }
        Item item = held.getItem();
        return item instanceof ItemHoe || item.getToolClasses(held).contains("hoe");
    }

    private static boolean isSowingItem(ItemStack held) {
        return !held.isEmpty() && held.getItem() instanceof IPlantable;
    }

    private static boolean isMiningTool(ItemStack held) {
        if (held.isEmpty()) {
            return false;
        }
        Item item = held.getItem();
        return item instanceof ItemTool
                || item instanceof ItemShears
                || !item.getToolClasses(held).isEmpty();
    }

    public static EnumFacing resolveTunnelDirection(EntityPlayer player) {
        return resolvePlayerFacing(player);
    }

    public static EnumFacing resolvePlayerFacing(EntityPlayer player) {
        if (player == null) {
            return EnumFacing.NORTH;
        }

        Vec3d look = player.getLookVec();
        if (look == null) {
            return player.getHorizontalFacing();
        }

        double absX = Math.abs(look.x);
        double absY = Math.abs(look.y);
        double absZ = Math.abs(look.z);
        if (absY >= absX && absY >= absZ) {
            return look.y >= 0.0D ? EnumFacing.UP : EnumFacing.DOWN;
        }
        if (absX >= absZ) {
            return look.x >= 0.0D ? EnumFacing.EAST : EnumFacing.WEST;
        }
        return look.z >= 0.0D ? EnumFacing.SOUTH : EnumFacing.NORTH;
    }

    private static boolean canTill(World world, BlockPos pos, EnumFacing face, ItemStack held) {
        if (world == null || pos == null || face == EnumFacing.DOWN) {
            return false;
        }
        if (!isTillingItem(held)) {
            return false;
        }
        if (!world.isAirBlock(pos.up())) {
            return false;
        }

        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block == Blocks.GRASS || block == Blocks.GRASS_PATH) {
            return true;
        }
        if (block == Blocks.DIRT) {
            BlockDirt.DirtType type = state.getValue(BlockDirt.VARIANT);
            return type == BlockDirt.DirtType.DIRT || type == BlockDirt.DirtType.COARSE_DIRT;
        }
        return false;
    }

    private static boolean canSow(World world, BlockPos pos, EnumFacing face, ItemStack held) {
        if (world == null || pos == null || face != EnumFacing.UP || !isSowingItem(held)) {
            return false;
        }
        if (!world.isAirBlock(pos.up())) {
            return false;
        }

        IPlantable plantable = (IPlantable) held.getItem();
        IBlockState soilState = world.getBlockState(pos);
        return soilState.getBlock().canSustainPlant(soilState, world, pos, EnumFacing.UP, plantable);
    }

    private static boolean canHarvest(World world, BlockPos pos, ItemStack held) {
        return held.isEmpty() && isHarvestableCrop(world, pos);
    }

    private static boolean applyTill(EntityPlayer player, World world, BlockPos pos, EnumFacing face, ItemStack held) {
        if (!canTill(world, pos, face, held)) {
            return false;
        }

        IBlockState state = world.getBlockState(pos);
        IBlockState targetState = null;
        Block block = state.getBlock();
        if (block == Blocks.GRASS || block == Blocks.GRASS_PATH) {
            targetState = Blocks.FARMLAND.getDefaultState();
        } else if (block == Blocks.DIRT) {
            BlockDirt.DirtType type = state.getValue(BlockDirt.VARIANT);
            if (type == BlockDirt.DirtType.COARSE_DIRT) {
                targetState = Blocks.DIRT.getDefaultState();
            } else if (type == BlockDirt.DirtType.DIRT) {
                targetState = Blocks.FARMLAND.getDefaultState();
            }
        }

        if (targetState == null) {
            return false;
        }

        world.playSound(null, pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.setBlockState(pos, targetState, 11);
        if (!player.capabilities.isCreativeMode && held.isItemStackDamageable()) {
            held.damageItem(1, player);
        }
        return true;
    }

    private static boolean applySow(EntityPlayer player, World world, BlockPos pos, EnumFacing face, ItemStack held) {
        if (!canSow(world, pos, face, held)) {
            return false;
        }

        EnumActionResult result = held.onItemUse(player, world, pos, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        return result == EnumActionResult.SUCCESS;
    }

    private static boolean isHarvestableCrop(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BlockCrops) {
            return ((BlockCrops) block).isMaxAge(state);
        }
        if (block == Blocks.NETHER_WART) {
            return state.getValue(BlockNetherWart.AGE) >= 3;
        }
        PropertyInteger ageProperty = findAgeProperty(state);
        if (ageProperty == null) {
            return false;
        }
        return state.getValue(ageProperty) >= Collections.max(ageProperty.getAllowedValues());
    }

    private static boolean applyHarvest(EntityPlayerMP player, World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        NonNullList<ItemStack> drops = NonNullList.create();
        block.getDrops(drops, world, pos, state, 0);
        ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, 0, 1.0F, false, player);

        if (block instanceof BlockCrops) {
            ItemStack seed = new ItemStack(getCropSeed((BlockCrops) block));
            shrinkMatchingDrop(drops, seed);
            world.setBlockState(pos, ((BlockCrops) block).withAge(0), 11);
        } else if (block == Blocks.NETHER_WART) {
            shrinkMatchingDrop(drops, new ItemStack(Items.NETHER_WART));
            world.setBlockState(pos, Blocks.NETHER_WART.getDefaultState().withProperty(BlockNetherWart.AGE, 0), 11);
        } else {
            PropertyInteger ageProperty = findAgeProperty(state);
            if (ageProperty == null) {
                return false;
            }
            int minAge = Collections.min(ageProperty.getAllowedValues());
            world.setBlockState(pos, state.withProperty(ageProperty, minAge), 11);
        }

        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, drop);
            }
        }
        return true;
    }

    private static void shrinkMatchingDrop(List<ItemStack> drops, ItemStack target) {
        if (target.isEmpty()) {
            return;
        }
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && ItemStack.areItemsEqual(drop, target)) {
                drop.shrink(1);
                if (drop.getCount() <= 0) {
                    drop.setCount(0);
                }
                return;
            }
        }
    }

    private static Method cropGetSeedMethod;

    private static Item getCropSeed(BlockCrops crop) {
        if (cropGetSeedMethod == null) {
            try {
                cropGetSeedMethod = BlockCrops.class.getDeclaredMethod("getSeed");
                cropGetSeedMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                return Items.WHEAT_SEEDS;
            }
        }
        try {
            return (Item) cropGetSeedMethod.invoke(crop);
        } catch (Exception e) {
            return Items.WHEAT_SEEDS;
        }
    }

    private static PropertyInteger findAgeProperty(IBlockState state) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property instanceof PropertyInteger && "age".equals(property.getName().toLowerCase(Locale.ROOT))) {
                return (PropertyInteger) property;
            }
        }
        return null;
    }

    public enum ChainAction {
        MINE,
        TILL,
        HARVEST,
        SOW
    }

    public static final class SelectionTarget {
        private final ChainAction action;
        private final List<BlockPos> positions;
        private final int totalCount;
        private final int hiddenCount;
        private final boolean countExact;

        public SelectionTarget(ChainAction action, List<BlockPos> positions) {
            this(action, positions, positions == null ? 0 : positions.size(), 0, true);
        }

        public SelectionTarget(ChainAction action, List<BlockPos> positions, int totalCount, int hiddenCount, boolean countExact) {
            this.action = action == null ? ChainAction.MINE : action;
            this.positions = positions == null ? Collections.emptyList() : positions;
            this.totalCount = Math.max(0, totalCount);
            this.hiddenCount = Math.max(0, hiddenCount);
            this.countExact = countExact;
        }

        public ChainAction getAction() {
            return this.action;
        }

        public List<BlockPos> getPositions() {
            return this.positions;
        }

        public List<BlockPos> getRenderedPositions(int renderLimit) {
            int effectiveRenderLimit = Math.max(1, renderLimit);
            if (this.positions.size() <= effectiveRenderLimit) {
                return this.positions;
            }
            return this.positions.subList(0, effectiveRenderLimit);
        }

        public int getTotalCount() {
            return this.totalCount;
        }

        public boolean isCountExact() {
            return this.countExact;
        }

        public int getRenderedCount(int renderLimit) {
            return Math.min(this.positions.size(), Math.max(1, renderLimit));
        }

        public int getHiddenCount(int renderLimit) {
            if (this.countExact) {
                return Math.max(0, this.totalCount - getRenderedCount(renderLimit));
            }
            return Math.max(this.hiddenCount, Math.max(0, this.totalCount - getRenderedCount(renderLimit)));
        }

    }

    @Desugar
    private record PreviewSelectionResult(ChainAction action, List<BlockPos> positions, int totalCount, int hiddenCount,
                                          boolean countExact) {
            private PreviewSelectionResult(ChainAction action, List<BlockPos> positions, int totalCount, int hiddenCount, boolean countExact) {
                this.action = action == null ? ChainAction.MINE : action;
                this.positions = positions == null ? Collections.emptyList() : positions;
                this.totalCount = Math.max(0, totalCount);
                this.hiddenCount = Math.max(0, hiddenCount);
                this.countExact = countExact;
            }

            private static PreviewSelectionResult empty(ChainAction action) {
                return new PreviewSelectionResult(action, Collections.emptyList(), 0, 0, true);
            }

            private static PreviewSelectionResult exact(ChainAction action, List<BlockPos> positions, int renderLimit) {
                int total = positions == null ? 0 : positions.size();
                int rendered = Math.min(total, Math.max(1, renderLimit));
                return new PreviewSelectionResult(action, positions, total, Math.max(0, total - rendered), true);
            }

            private static PreviewSelectionResult inexact(ChainAction action, List<BlockPos> positions, int totalCount, int hiddenCount) {
                return new PreviewSelectionResult(action, positions, totalCount, hiddenCount, false);
            }
        }
}
