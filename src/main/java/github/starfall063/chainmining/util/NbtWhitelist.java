package github.starfall063.chainmining.util;

import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class NbtWhitelist {
    private static final NbtWhitelist EMPTY = new NbtWhitelist(Collections.emptyList());
    private final List<String[]> paths;

    private NbtWhitelist(List<String[]> paths) {
        this.paths = paths;
    }

    static NbtWhitelist from(String[] keys) {
        if (keys == null || keys.length == 0) return EMPTY;
        List<String[]> parsed = new ArrayList<>();
        for (String key : keys) {
            if (key == null) continue;
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) parsed.add(trimmed.split("\\."));
        }
        return parsed.isEmpty() ? EMPTY : new NbtWhitelist(Collections.unmodifiableList(parsed));
    }

    boolean isEmpty() {
        return paths.isEmpty();
    }

    NBTTagCompound extract(NBTTagCompound source) {
        if (source == null || source.isEmpty() || paths.isEmpty()) return null;
        NBTTagCompound result = new NBTTagCompound();
        for (String[] path : paths) copyByPath(source, result, path, 0);
        return result.isEmpty() ? null : result;
    }

    private static void copyByPath(NBTTagCompound source, NBTTagCompound result, String[] path, int index) {
        String key = path[index];
        if (!source.hasKey(key)) return;
        if (index == path.length - 1) {
            result.setTag(key, source.getTag(key).copy());
            return;
        }
        NBTTagCompound subResult = new NBTTagCompound();
        result.setTag(key, subResult);
        copyByPath(source.getCompoundTag(key), subResult, path, index + 1);
    }
}
