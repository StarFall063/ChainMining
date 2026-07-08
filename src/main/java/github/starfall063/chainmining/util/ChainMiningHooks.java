package github.starfall063.chainmining.util;

import github.starfall063.chainmining.ChainMiningConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public final class ChainMiningHooks {
    public static boolean isToolBlacklisted(ItemStack tool) {
        if (tool.isEmpty()) return false;
        String id = Objects.requireNonNull(tool.getItem().getRegistryName()).toString();
        for (String entry : ChainMiningConfig.SERVER.chainMiningToolBlackList) {
            if (entry.trim().equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    public static boolean isBlockBlacklisted(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        String regName = Objects.requireNonNull(block.getRegistryName()).toString();
        int meta = block.getMetaFromState(state);
        String fullId = regName + ":" + meta;
        for (String entry : ChainMiningConfig.SERVER.chainMiningBlockBlackList) {
            if (entry.trim().equalsIgnoreCase(regName) || entry.trim().equalsIgnoreCase(fullId)) return true;
        }
        return false;
    }

    public static boolean canChainMineBlock(World world, BlockPos pos, IBlockState state, EntityPlayer player, ItemStack held) {
        if (world == null || pos == null || state == null || state.getBlock().isAir(state, world, pos)) return false;

        if (player != null && player.capabilities.isCreativeMode) return true;

        if (ChainMiningConfig.SERVER.chainMiningIgnoreHeldItem) return true;

        if (state.getMaterial().isToolNotRequired()) return true;

        if (held == null || held.isEmpty()) return state.getBlock().getHarvestLevel(state) <= 0;
        return  held.getItem().canHarvestBlock(state, held);
    }

    public static List<BlockPos> scanBlocks(World world, BlockPos startPos, IBlockState startState, BlockIdentity sourceId, BlockMatchMode matchMode, int maxBlocks, int neighborRange, EntityPlayer player, ChainShapeMode shapeMode, EnumFacing hitFace) {
        if (hitFace == null && player != null) {
            hitFace = player.getHorizontalFacing();
        }

        ItemStack tool = (player != null) ? player.getHeldItemMainhand() : ItemStack.EMPTY;
        return switch (shapeMode) {
            case PLANE ->
                    scanDirectional(world, startPos, hitFace, startState, sourceId, matchMode, maxBlocks, player, tool);
            case TUNNEL ->
                    scanTunnel(world, startPos, hitFace, startState, sourceId, matchMode, maxBlocks, player, tool);
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
            ItemStack tool) {
        List<BlockPos> result = new ArrayList<>();
        result.add(origin);
        if (face == null) return result;
        EnumFacing dir = face.getOpposite();
        for (int depth = 1; depth < maxBlocks; depth++) {
            BlockPos next = origin.offset(dir, depth);
            IBlockState state = world.getBlockState(next);
            if (state.getBlock().isAir(state, world, next)) break;
            if (isBlockBlacklisted(world, next, state)) continue;
            if (!canChainMineBlock(world, next, state, player, tool)) continue;
            if (!sourceId.matches(world, next, state, matchMode)) break;
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
            ItemStack tool) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> added = new HashSet<>();
        if (face == null) {
            result.add(startPos);
            return result;
        }
        EnumFacing dir = face.getOpposite();
        EnumFacing.Axis axis = dir.getAxis();
        for (int depth = 0; result.size() < maxBlocks; depth++) {
            BlockPos center = startPos.offset(dir, depth);
            boolean anyAdded = false;
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
                    if (state.getBlock().isAir(state, world, candidate)) continue;
                    if (isBlockBlacklisted(world, candidate, state)) continue;
                    if (!canChainMineBlock(world, candidate, state, player, tool)) continue;
                    if (!sourceId.matches(world, candidate, state, matchMode)) continue;
                    result.add(candidate);
                    anyAdded = true;
                }
            }
            if (!anyAdded) break;
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

//    private static boolean isSecuredBlock()

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

    public static void executeChainMining(EntityPlayer player, List<BlockPos> blocks, ItemStack tool) {
        World world = player.world;
        BlockPos sourcePos = blocks.get(0);
        int totalXp = 0;
        Map<ItemKey, Integer> dropMap = new LinkedHashMap<>();

        for (BlockPos pos : blocks) {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (!player.capabilities.isCreativeMode) {
                if (state.getBlockHardness(world, pos) < 0) continue;
                if (!ChainMiningConfig.SERVER.chainMiningIgnoreHeldItem) {
                    if (!tool.isEmpty() && !tool.canHarvestBlock(state)) continue;
                }
            }
            if (player.capabilities.isCreativeMode) {
                world.setBlockToAir(pos);
                continue;
            }

            player.addExhaustion((float) ChainMiningConfig.SERVER.chainMiningExhaustionPerBlock);

            if (ChainMiningConfig.SERVER.chainMiningIgnoreHeldItem || tool.isEmpty() || tool.canHarvestBlock(state)) {
                int fortune = 0;
                if (Enchantments.FORTUNE != null) {
                    fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
                }
                List<ItemStack> drops = block.getDrops(world, pos, state, fortune);
                for (ItemStack drop : drops) {
                    if (drop.isEmpty()) continue;
                    ItemKey key = new ItemKey(drop);
                    dropMap.merge(key, drop.getCount(), Integer::sum);
                }

                totalXp += block.getExpDrop(state, world, pos, fortune);
            }

            world.setBlockToAir(pos);

            if (!tool.isEmpty()) {
                tool.onBlockDestroyed(world, state, pos, player);
                if (tool.isEmpty()) break;
            }
        }

        for (Map.Entry<ItemKey, Integer> entry : dropMap.entrySet()) {
           int total = entry.getValue();
           ItemStack stack = entry.getKey().toStack();
           while (total > 0) {
               int size = Math.min(total, stack.getMaxStackSize());
               stack.setCount(size);
               Block.spawnAsEntity(world, sourcePos, stack.copy());
               total -= size;
           }
       }

        if (totalXp > 0) {
            world.spawnEntity(new EntityXPOrb(world, sourcePos.getX() + 0.5, sourcePos.getY() + 0.5, sourcePos.getZ() + 0.5, totalXp));
        }
    }

    private static final class ItemKey {
        private final Item item;
        private final int meta;
        private final NBTTagCompound tag;

        ItemKey(ItemStack stack) {
            this.item = stack.getItem();
            this.meta = stack.getMetadata();
            this.tag = stack.hasTagCompound() ? Objects.requireNonNull(stack.getTagCompound()).copy() : null;
        }

        ItemStack toStack() {
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
