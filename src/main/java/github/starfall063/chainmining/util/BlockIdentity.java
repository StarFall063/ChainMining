package github.starfall063.chainmining.util;

import github.starfall063.chainmining.ChainMiningConfig;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public final class BlockIdentity {
    private static volatile String[] lastNbtKeys = null;
    private static volatile NbtWhitelist nbtWhitelist = NbtWhitelist.from(null);
    private static volatile String[] lastEquivalentGroups = null;
    private static volatile Map<String, String> equivalentBlockIds = Collections.emptyMap();
    private static volatile Set<String> equivalentMetaWildcards = Collections.emptySet();
    private final String blockId;
    private final int meta;
    private final NBTTagCompound nbt;

    private BlockIdentity(String blockId, int meta, NBTTagCompound nbt) {
        this.blockId = blockId;
        this.meta = meta;
        this.nbt = nbt;
    }

    static void refreshWhitelist() {
        String[] keys = ChainMiningConfig.SERVER.chainMiningNbtMatchKeys;
        if (keys == lastNbtKeys) return;
        lastNbtKeys = keys;
        nbtWhitelist = NbtWhitelist.from(keys);
    }

    private static NBTTagCompound getBlockNbt(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te == null) return null;
        NBTTagCompound nbt = new NBTTagCompound();
        te.writeToNBT(nbt);
        return nbt;
    }

    public static BlockIdentity from(World world, BlockPos pos, IBlockState state, BlockMatchMode matchMode) {
        Block block = state.getBlock();
        ResourceLocation id = block.getRegistryName();
        if (id == null) return null;

        String rawBlockId = normalizeVanillaBlockId(id.toString());
        String blockId = normalizeBlockId(rawBlockId);

        int meta = matchMode.shouldMatchMeta() && !hasEquivalentMetaWildcard(rawBlockId) ? block.getMetaFromState(state) : -1;

        NBTTagCompound nbt = null;
        if (matchMode.shouldMatchNbt()) {
            refreshWhitelist();
            if (!nbtWhitelist.isEmpty()) nbt = nbtWhitelist.extract(getBlockNbt(world, pos));
        }

        return new BlockIdentity(blockId, meta, nbt);
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
                        String blockPattern = wildcardMeta ? trimmed.substring(0, trimmed.length() - 2) : trimmed;
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
            equivalentBlockIds = Collections.unmodifiableMap(aliases);
            equivalentMetaWildcards = Collections.unmodifiableSet(new HashSet<>(metaWildcardIds));
            lastEquivalentGroups = configured == null ? null : configured.clone();
        }
    }

    private static boolean hasEquivalentMetaWildcard(String blockId) {
        refreshEquivalentGroups();
        return equivalentMetaWildcards.contains(blockId);
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

    public boolean matches(World world, BlockPos pos, IBlockState state, BlockMatchMode matchMode) {
        if (matchMode.shouldMatchNbt()) refreshWhitelist();

        ResourceLocation id = state.getBlock().getRegistryName();
        if (id == null) return false;
        String rawBlockId = normalizeVanillaBlockId(id.toString());
        String targetBockId = normalizeBlockId(rawBlockId);
        if (!this.blockId.equals(targetBockId)) return false;

        if (matchMode.shouldMatchMeta() && this.meta >= 0 && !hasEquivalentMetaWildcard(rawBlockId) && this.meta != state.getBlock().getMetaFromState(state))
            return false;
        if (matchMode.shouldMatchNbt()) {
            if (nbtWhitelist.isEmpty()) return this.nbt == null;
            NBTTagCompound otherNbt = nbtWhitelist.extract(getBlockNbt(world, pos));
            if (!Objects.equals(this.nbt, otherNbt)) return false;
        }
        return true;
    }
}
