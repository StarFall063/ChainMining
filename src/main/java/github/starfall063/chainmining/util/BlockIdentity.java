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
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BlockIdentity {
    private final String blockId;
    private final int meta;
    private final NBTTagCompound nbt;

    private static final Set<String> KEY_WHITELIST = new HashSet<>();
    private static volatile String[] lastNbtKeys = null;
    private static volatile String[] lastEquivalentGroups = null;
    private static volatile Map<String, String> equivalentBlockIds = Collections.emptyMap();
    private static volatile Set<String> equivalentMetaWildcards = Collections.emptySet();

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

        String blockId = normalizeBlockId(id.toString());

        int meta = matchMode.shouldMatchMeta() && !hasEquivalentMetaWildcard(blockId)
                ? block.getMetaFromState(state) : -1;

        NBTTagCompound nbt = null;
        if (matchMode.shouldMatchNbt()) {
            refreshWhitelist();
            NBTTagCompound rawNbt = getBlockNbt(world, pos);
            nbt = extractWhitelistNbt(rawNbt);
        }

        return new BlockIdentity(blockId, meta, nbt);
    }

    public boolean matches(World world, BlockPos pos, IBlockState state, BlockMatchMode matchMode) {
        if (matchMode.shouldMatchNbt()) refreshWhitelist();
        
        ResourceLocation id = state.getBlock().getRegistryName();
        if (id == null) return false;
        String targetBockId = normalizeBlockId(id.toString());
        if (!this.blockId.equals(targetBockId)) return false;

        if (matchMode.shouldMatchMeta() && this.meta >= 0
                && this.meta != state.getBlock().getMetaFromState(state)) return false;
        if (matchMode.shouldMatchNbt()) {
            NBTTagCompound rawNbt = getBlockNbt(world, pos);
            NBTTagCompound otherNbt = extractWhitelistNbt(rawNbt);
            if (!Objects.equals(this.nbt, otherNbt)) return false;
        }
        return true;
    }

    private static String normalizeBlockId(String blockId) {
        String normalized = normalizeVanillaBlockId(blockId);
        refreshEquivalentGroups();
        String equivalent = equivalentBlockIds.get(normalized);
        return equivalent == null ? normalized : equivalent;
    }

    private static String normalizeVanillaBlockId(String blockId) {
        if ("minecraft:lit_redstone_ore".equals(blockId)) {
            return "minecraft:redstone_ore";
        }
        return blockId;
    }

    private static void refreshEquivalentGroups() {
        String[] configured = ChainMiningConfig.SERVER.chainMiningEquivalentBlockGroups;
        if (Arrays.equals(configured, lastEquivalentGroups)) return;

        synchronized (BlockIdentity.class) {
            if (Arrays.equals(configured, lastEquivalentGroups)) return;

            Map<String, String> aliases = new HashMap<>();
            Set<String> metaWildcardIds = new HashSet<>();
            if (configured != null) {
                for (String group : configured) {
                    if (group == null) continue;
                    Set<String> validIds = new LinkedHashSet<>();
                    String[] entries = group.split(",", -1);
                    for (String entry : entries) {
                        String trimmed = entry.trim();
                        if (trimmed.isEmpty()) continue;
                        boolean wildcardMeta = trimmed.endsWith(":*");
                        String blockPattern = wildcardMeta
                                ? trimmed.substring(0, trimmed.length() - 2) : trimmed;
                        Set<String> matchedIds = new LinkedHashSet<>();
                        if (blockPattern.indexOf('*') >= 0) {
                            for (ResourceLocation registeredId : Block.REGISTRY.getKeys()) {
                                String id = normalizeVanillaBlockId(registeredId.toString());
                                if (matchesWildcard(blockPattern, id)) matchedIds.add(id);
                            }
                        } else {
                            String id = normalizeVanillaBlockId(blockPattern);
                            try {
                                if (Block.REGISTRY.getObject(new ResourceLocation(id)) != null) {
                                    matchedIds.add(id);
                                }
                            } catch (IllegalArgumentException ignored) {
                                // Ignore malformed registry IDs in configuration.
                            }
                        }
                        validIds.addAll(matchedIds);
                        if (wildcardMeta) metaWildcardIds.addAll(matchedIds);
                    }
                    if (validIds.size() < 2) continue;

                    String canonical = null;
                    Set<String> roots = new HashSet<>();
                    for (String id : validIds) {
                        String root = resolveAlias(aliases, id);
                        roots.add(root);
                        if (canonical == null) canonical = root;
                    }
                    for (Map.Entry<String, String> alias : aliases.entrySet()) {
                        if (roots.contains(resolveAlias(aliases, alias.getKey()))) {
                            alias.setValue(canonical);
                        }
                    }
                    for (String id : validIds) {
                        aliases.put(id, canonical);
                    }
                }
            }
            Set<String> canonicalMetaWildcards = new HashSet<>();
            for (String id : metaWildcardIds) {
                String canonical = resolveAlias(aliases, id);
                canonicalMetaWildcards.add(canonical);
            }
            equivalentBlockIds = Collections.unmodifiableMap(aliases);
            equivalentMetaWildcards = Collections.unmodifiableSet(canonicalMetaWildcards);
            lastEquivalentGroups = configured == null ? null : configured.clone();
        }
    }

    private static boolean hasEquivalentMetaWildcard(String canonicalId) {
        refreshEquivalentGroups();
        return equivalentMetaWildcards.contains(canonicalId);
    }

    private static String resolveAlias(Map<String, String> aliases, String id) {
        String current = id;
        String next = aliases.get(current);
        while (next != null && !next.equals(current)) {
            current = next;
            next = aliases.get(current);
        }
        return current;
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
}
