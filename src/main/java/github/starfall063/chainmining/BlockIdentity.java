package github.starfall063.chainmining;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public final class BlockIdentity {
    private final Block block;
    private final StateFingerprint fingerprint;
    private final NBTTagCompound normalizedTileData;

    private BlockIdentity(Block block, StateFingerprint fingerprint, NBTTagCompound normalizedTileData) {
        this.block = block;
        this.fingerprint = fingerprint;
        this.normalizedTileData = normalizedTileData;
    }

    public static BlockIdentity from(World world, BlockPos pos, IBlockState state, BlockMatchMode matchMode) {
        if (world == null || pos == null || state == null || matchMode == null) {
            return null;
        }
        Block block = state.getBlock();
        StateFingerprint fingerprint = matchMode.shouldMatchMeta() ? StateFingerprint.from(state) : null;
        NBTTagCompound tileData = matchMode.shouldMatchNbt() ? createNormalizedTileEntityData(world, pos) : null;
        return new BlockIdentity(block, fingerprint, tileData);
    }

    public boolean matches(World world, BlockPos pos, IBlockState state, BlockMatchMode matchMode) {
        if (world == null || pos == null || state == null || matchMode == null) {
            return false;
        }
        if (state.getBlock() != this.block) {
            return false;
        }
        if (matchMode.shouldMatchMeta() && this.fingerprint != null) {
            if (!this.fingerprint.equals(StateFingerprint.from(state))) {
                return false;
            }
        }
        if (matchMode.shouldMatchNbt()) {
            NBTTagCompound currentData = createNormalizedTileEntityData(world, pos);
            if (!Objects.equals(this.normalizedTileData, currentData)) {
                return false;
            }
        }
        return true;
    }

    private static NBTTagCompound createNormalizedTileEntityData(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (te == null) {
            return null;
        }
        NBTTagCompound tag = te.writeToNBT(new NBTTagCompound());
        stripVolatileKeys(tag);
        return tag;
    }

    private static void stripVolatileKeys(NBTTagCompound tag) {
        String[] volatileKeys = {
                "active", "energy", "progress", "heat", "ticker",
                "x", "y", "z", "owner", "glowing", "powered", "lit",
                "cooldown", "timer", "lastChange", "lastRecipe", "updateCount"
        };
        for (String key : volatileKeys) {
            tag.removeTag(key);
        }
        for (String key : new ArrayList<>(tag.getKeySet())) {
            NBTBase value = tag.getTag(key);
            if (value instanceof NBTTagCompound) {
                NBTTagCompound child = (NBTTagCompound) value;
                stripVolatileKeys(child);
                if (child.getKeySet().isEmpty()) {
                    tag.removeTag(key);
                }
            } else if (value instanceof NBTTagList) {
                NBTTagList list = (NBTTagList) value;
                for (int i = 0; i < list.tagCount(); i++) {
                    if (list.get(i) instanceof NBTTagCompound) {
                        stripVolatileKeys(list.getCompoundTagAt(i));
                    }
                }
            }
        }
    }

    static final class StateFingerprint {
        private final Map<String, String> properties;

        private StateFingerprint(Map<String, String> properties) {
            this.properties = properties;
        }

        static StateFingerprint from(IBlockState state) {
            Map<String, String> props = new LinkedHashMap<>();
            for (IProperty<?> property : state.getPropertyKeys()) {
                if (isOrientationProperty(property)) {
                    continue;
                }
                props.put(property.getName(), state.getValue(property).toString());
            }
            return new StateFingerprint(Collections.unmodifiableMap(props));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof StateFingerprint)) {
                return false;
            }
            return this.properties.equals(((StateFingerprint) obj).properties);
        }

        @Override
        public int hashCode() {
            return this.properties.hashCode();
        }

        private static boolean isOrientationProperty(IProperty<?> property) {
            if (property instanceof PropertyDirection) {
                return true;
            }
            String name = property.getName().toLowerCase(Locale.ROOT);
            return name.contains("facing") || name.contains("axis") || name.contains("rotation");
        }
    }
}
