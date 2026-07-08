package github.starfall063.chainmining.util;

import github.starfall063.chainmining.ChainMiningConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class BlockIdentity {
    private final String blockId;
    private final int meta;
    private final NBTTagCompound nbt;

    private static final Set<String> KEY_WHITELIST = new HashSet<>();
    private static volatile String[] lastNbtKeys = null;

    static void refreshWhitelist() {
        String[] keys = ChainMiningConfig.SERVER.chainMiningNbtMatchKeys;
        if (keys == lastNbtKeys) return;
        lastNbtKeys = keys;
        KEY_WHITELIST.clear();
        for (String key : keys) {
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) {
                KEY_WHITELIST.add(trimmed);
            }
        }
    }

    private static NBTTagCompound getBlockNbt(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return null;
        NBTTagCompound nbt = new NBTTagCompound();
        te.writeToNBT(nbt);
        return nbt;
    }

    private static NBTTagCompound extractWhitelistNbt(NBTTagCompound nbt) {
        if (nbt == null || nbt.isEmpty()) return null;
        NBTTagCompound result = new NBTTagCompound();
        for (String key : KEY_WHITELIST) {
            String[] parts = key.split("\\.");
            copyByPath(nbt, result, parts);
        }
        return result.isEmpty() ? null : result;
    }

    private static void copyByPath(NBTTagCompound source, NBTTagCompound result, String[] path) {
        copyByPath(source, result, path, 0);
    }

    private static void copyByPath(NBTTagCompound source, NBTTagCompound result, String[] path, int index) {
        String key = path[index];
        if (!source.hasKey(key)) return;

        boolean isLast = (index == path.length - 1);
        if (isLast) {
            result.setTag(key, source.getTag(key).copy());
        } else {
            NBTTagCompound subSource = source.getCompoundTag(key);
            NBTTagCompound subResult = new NBTTagCompound();
            result.setTag(key, subResult);
            copyByPath(subSource, subResult, path, index + 1);
        }
    }

    private BlockIdentity(String blockId, int meta, NBTTagCompound nbt) {
        this.blockId = blockId;
        this.meta = meta;
        this.nbt = nbt;
    }

    public static BlockIdentity from(World world, BlockPos pos, IBlockState state, BlockMatchMode matchMode) {
        Block block = state.getBlock();
        ResourceLocation id = block.getRegistryName();
        if (id == null) return null;

        String blockId = id.toString();
        blockId = normalizeBlockId(blockId);

        int meta = matchMode.shouldMatchMeta() ? block.getMetaFromState(state) : -1;

        NBTTagCompound nbt = null;
        if (matchMode.shouldMatchNbt()) {
            refreshWhitelist();
            NBTTagCompound rawNbt = getBlockNbt(world, pos);
            nbt = extractWhitelistNbt(rawNbt);
        }

        return new BlockIdentity(blockId, meta, nbt);
    }

    public boolean matches(World world, BlockPos pos, IBlockState state, BlockMatchMode matchMode) {
        ResourceLocation id = state.getBlock().getRegistryName();
        if (id == null) return false;
        String targetBockId = normalizeBlockId(id.toString());
        if (!this.blockId.equals(targetBockId)) return false;

        if (matchMode.shouldMatchMeta() && this.meta != state.getBlock().getMetaFromState(state)) return false;
        if (matchMode.shouldMatchNbt()) {
            NBTTagCompound rawNbt = getBlockNbt(world, pos);
            NBTTagCompound otherNbt = extractWhitelistNbt(rawNbt);
            if (!Objects.equals(this.nbt, otherNbt)) return false;
        }
        return true;
    }

    private static String normalizeBlockId(String blockId) {
        if ("minecraft:lit_redstone_ore".equals(blockId)) {
            return "minecraft:redstone_ore";
        }
        return blockId;
    }
}
