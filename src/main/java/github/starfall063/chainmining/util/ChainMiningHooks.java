package github.starfall063.chainmining.util;

import github.starfall063.chainmining.ChainMiningConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import java.util.*;

public final class ChainMiningHooks {
    public static boolean isToolBlacklisted(ItemStack tool) {
        if (tool.isEmpty()) return false;
        ResourceLocation id = tool.getItem().getRegistryName();
        if (id == null) return false;
        String name = id.toString();
        for (String entry : ChainMiningConfig.SERVER.chainMiningToolBlackList) {
            if (entry.trim().equalsIgnoreCase(name)) return true;
            if (matchesWildcard(entry.trim(), name)) return true;
        }
        return false;
    }

    public static boolean isBlockBlacklisted(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        ResourceLocation id = block.getRegistryName();
        if (id == null) return false;
        String regName = id.toString();
        int meta = block.getMetaFromState(state);
        String fullId = regName + ":" + meta;
        for (String entry : ChainMiningConfig.SERVER.chainMiningBlockBlackList) {
            String e = entry.trim();
            if (e.isEmpty()) continue;
            if (matchesWildcard(e, regName) || matchesWildcard(e, fullId)) return true;
        }
        return false;
    }

    public static boolean canChainMineBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player, ItemStack held) {
        if (world == null || pos == null || state == null || state.getBlock().isAir(state, world, pos)) return false;

        if (player != null && player.capabilities.isCreativeMode) return true;

       boolean canHarvest = player != null && ForgeHooks.canHarvestBlock(state.getBlock(), player, world, pos);

        if (player != null && state.getPlayerRelativeBlockHardness(player, world, pos) <= 0F) return false;
       if (ChainMiningConfig.SERVER.chainMiningIgnoreHeldItem) return true;
       return canHarvest;
    }

    public static List<BlockPos> scanBlocks(World world, BlockPos startPos, IBlockState startState, BlockIdentity sourceId, BlockMatchMode matchMode, int maxBlocks, int neighborRange, EntityPlayer player, ChainShapeMode shapeMode, EnumFacing hitFace) {
        if (hitFace == null && player != null) {
            hitFace = player.getHorizontalFacing();
        }

        ItemStack tool = (player != null) ? player.getHeldItemMainhand() : ItemStack.EMPTY;
        return switch (shapeMode) {
            case PLANE ->
                    scanDirectional(world, startPos, hitFace, startState, sourceId, matchMode, maxBlocks, player, tool, neighborRange);
            case TUNNEL ->
                    scanTunnel(world, startPos, hitFace, startState, sourceId, matchMode, maxBlocks, player, tool, neighborRange);
            default ->
                    scanShapeless(world, startPos, startState, sourceId, matchMode, maxBlocks, neighborRange, player, tool);
        };
    }

    private static List<BlockPos> scanDirectional(
            World world,
            BlockPos origin,
            EnumFacing face,
            IBlockState startState,
            BlockIdentity sourceId,
            BlockMatchMode matchMode,
            int maxBlocks,
            EntityPlayer player,
            ItemStack tool,
            int neighborRange) {
        List<BlockPos> result = new ArrayList<>();
        result.add(origin);

        if (face == null) return result;

        EnumFacing dir = face.getOpposite();
        int gap = 0;
        for (int depth = 1; result.size() < maxBlocks; depth++) {
            BlockPos next = origin.offset(dir, depth);
            IBlockState state = world.getBlockState(next);

            if (state.getBlock().isAir(state, world, next)
                    || isBlockBlacklisted(world, next, state)
                    || !canChainMineBlock(world, next, state, player, tool)
                    || !sourceId.matches(world, next, state, matchMode)) {
                gap++;
                if (gap > neighborRange) break;
                continue;
            }
            gap = 0;
            result.add(next);
        }
        return result;
    }

    private static List<BlockPos> scanTunnel(
            World world,
            BlockPos startPos,
            EnumFacing face,
            IBlockState startState,
            BlockIdentity sourceId,
            BlockMatchMode matchMode,
            int maxBlocks,
            EntityPlayer player,
            ItemStack tool,
            int neighborRange) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> added = new HashSet<>();
        if (face == null) {
            result.add(startPos);
            return result;
        }
        result.add(startPos);
        added.add(startPos);

        EnumFacing dir = face.getOpposite();
        EnumFacing.Axis axis = dir.getAxis();
        int gap = 0;

        for (int depth = 0; result.size() < maxBlocks; depth++) {
            BlockPos center = startPos.offset(dir, depth);
            boolean layerHit = false;

            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (result.size() >= maxBlocks) break;
                    BlockPos candidate = switch (axis) {
                        case X -> center.add(0, a, b);
                        case Y -> center.add(a, 0, b);
                        default -> center.add(a, b, 0);
                    };

                    if (!added.add(candidate)) continue;

                    IBlockState state = world.getBlockState(candidate);

                    if (state.getBlock().isAir(state, world, candidate)
                            || isBlockBlacklisted(world, candidate, state)
                            || !canChainMineBlock(world, candidate, state, player, tool)
                            || !sourceId.matches(world, candidate, state, matchMode)) continue;
                    result.add(candidate);
                    layerHit = true;
                }
            }
            if (depth > 0) {
                if (layerHit) gap = 0;
                else { gap++; if (gap > neighborRange) break; }
            }
        }
        return result;
    }

    private static List<BlockPos> scanShapeless(
            World world,
            BlockPos startPos,
            IBlockState startState,
            BlockIdentity sourceId,
            BlockMatchMode matchMode,
            int maxBlocks,
            int neighborRange,
            EntityPlayer player,
            ItemStack tool) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> result = new ArrayList<>();
        visited.add(startPos);
        queue.add(startPos);
        result.add(startPos);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            BlockPos current = queue.poll();
            for (BlockPos next : getNeighbors(current, neighborRange)) {
                if (visited.contains(next)) continue;
                visited.add(next);
                IBlockState nextState = world.getBlockState(next);
                if (nextState.getBlock().isAir(nextState, world, next)) continue;
                if (isBlockBlacklisted(world, next, nextState)) continue;
                if (!canChainMineBlock(world, next, nextState, player, tool)) continue;
                if (!sourceId.matches(world, next, nextState, matchMode)) continue;
                if (result.size() >= maxBlocks) break;
                queue.add(next);
                result.add(next);
            }
            if (result.size() >= maxBlocks) break;
        }
        return result;
    }

    private static List<BlockPos> getNeighbors(BlockPos pos, int range) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    neighbors.add(pos.add(x, y, z));
                }
            }
        }
        neighbors.sort(Comparator.comparingDouble(p -> p.distanceSq(pos)));
        return neighbors;
    }

    public static void executeChainMining(EntityPlayerMP player, List<BlockPos> blocks, ItemStack tool) {
        World world = player.world;
        for (BlockPos pos : blocks) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock().isAir(state, world, pos)) continue;

            if (!player.capabilities.isCreativeMode) {
                if (player.getFoodStats().getFoodLevel() < ChainMiningConfig.SERVER.chainMiningMinFoodLevel) break;
            }

            player.interactionManager.tryHarvestBlock(pos);
            if (!player.capabilities.isCreativeMode) {
                player.addExhaustion((float) ChainMiningConfig.SERVER.chainMiningExhaustionPerBlock);
            }
       }
    }

    private static boolean matchesWildcard(String pattern, String text) {
        String[] parts = pattern.split("\\*", -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) regex.append(".*");
            regex.append(java.util.regex.Pattern.quote(parts[i]));
        }
        return text.matches(regex.toString());
    }

    public static final class ItemKey {
        private final Item item;
        private final int meta;
        private final NBTTagCompound tag;

        public ItemKey(ItemStack stack) {
            this.item = stack.getItem();
            this.meta = stack.getMetadata();
            this.tag = stack.hasTagCompound() ? Objects.requireNonNull(stack.getTagCompound()).copy() : null;
        }

        public ItemStack toStack() {
            ItemStack stack = new ItemStack(item, 1, meta);
            if (tag != null) stack.setTagCompound(tag.copy());
            return stack;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof ItemKey other)) return false;
            return this.item == other.item && this.meta == other.meta && Objects.equals(this.tag, other.tag);
        }

        @Override
        public int hashCode() {
            int hash = item.hashCode();
            hash = 31 * hash + meta;
            hash = 31 * hash + (tag != null ? tag.hashCode() : 0);
            return hash;
        }
    }
}
