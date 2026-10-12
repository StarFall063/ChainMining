package github.starfall063.chainmining.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

final class ShapelessScanFrontier {
    private static final int[][] INITIAL_OFFSETS = new int[17][];
    private final int radius;
    private final BlockPos origin;
    private final Long2ObjectOpenHashMap<Row> rows = new Long2ObjectOpenHashMap<>();
    private int[] offsets = new int[64];
    private int offsetCount;
    private boolean firstExpansion = true;

    ShapelessScanFrontier(BlockPos origin, int radius) {
        if (radius < 1 || radius > 16) throw new IllegalArgumentException("Neighbor radius must be 1-16");
        this.radius = radius;
        this.origin = origin.toImmutable();
        rows.put(rowKey(origin.getY(), origin.getZ()), new Row(origin.getX(), origin.getX()));
    }

    List<BlockPos> expand(BlockPos center) {
        if (firstExpansion && origin.equals(center)) {
            firstExpansion = false;
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    rows.put(rowKey(y, z), new Row(center.getX() - radius, center.getX() + radius));
                }
            }
            int[] initial = initialOffsets(radius);
            return positions(center, initial, initial.length);
        }
        firstExpansion = false;
        offsetCount = 0;
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                long key = rowKey(y, z);
                Row row = rows.get(key);
                if (row == null) {
                    collect(minX, maxX, y, z, center);
                    rows.put(key, new Row(minX, maxX));
                } else {
                    row.expand(minX, maxX, y, z, center);
                }
            }
        }

        Arrays.sort(offsets, 0, offsetCount);
        return positions(center, offsets, offsetCount);
    }

    private static List<BlockPos> positions(BlockPos center, int[] values, int count) {
        return new AbstractList<BlockPos>() {
            @Override
            public BlockPos get(int index) {
                if (index < 0 || index >= count) throw new IndexOutOfBoundsException();
                int offset = values[index];
                return center.add(((offset >>> 12) & 63) - 16,
                        ((offset >>> 6) & 63) - 16, (offset & 63) - 16);
            }

            @Override
            public int size() {
                return count;
            }
        };
    }

    private static synchronized int[] initialOffsets(int radius) {
        int[] cached = INITIAL_OFFSETS[radius];
        if (cached != null) return cached;
        int side = 2 * radius + 1;
        int[] values = new int[side * side * side - 1];
        int index = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x != 0 || y != 0 || z != 0) values[index++] = encodeOffset(x, y, z);
                }
            }
        }
        Arrays.sort(values);
        INITIAL_OFFSETS[radius] = values;
        return values;
    }

    private static int encodeOffset(int dx, int dy, int dz) {
        int distance = dx * dx + dy * dy + dz * dz;
        return (distance << 18) | ((dx + 16) << 12) | ((dy + 16) << 6) | (dz + 16);
    }

    private static long rowKey(int y, int z) {
        return ((long) y << 32) | (z & 0xFFFFFFFFL);
    }

    private void collect(int minX, int maxX, int y, int z, BlockPos center) {
        int dy = y - center.getY();
        int dz = z - center.getZ();
        for (int x = minX; x <= maxX; x++) {
            int dx = x - center.getX();
            if (offsetCount == offsets.length) offsets = Arrays.copyOf(offsets, offsets.length * 2);
            offsets[offsetCount++] = encodeOffset(dx, dy, dz);
        }
    }

    private final class Row {
        private int[] bounds;
        private int size = 2;

        Row(int minX, int maxX) {
            bounds = new int[]{minX, maxX, 0, 0};
        }

        void expand(int minX, int maxX, int y, int z, BlockPos center) {
            int first = 0;
            while (first < size && bounds[first + 1] < minX - 1) first += 2;
            if (first < size && bounds[first] <= minX && bounds[first + 1] >= maxX) return;
            int end = first;
            int cursor = minX;
            int mergedMin = minX;
            int mergedMax = maxX;
            while (end < size && bounds[end] <= maxX + 1) {
                if (cursor < bounds[end]) collect(cursor, Math.min(maxX, bounds[end] - 1), y, z, center);
                cursor = Math.max(cursor, bounds[end + 1] + 1);
                mergedMin = Math.min(mergedMin, bounds[end]);
                mergedMax = Math.max(mergedMax, bounds[end + 1]);
                end += 2;
            }
            if (cursor <= maxX) collect(cursor, maxX, y, z, center);

            int newSize = size - (end - first) + 2;
            if (newSize > bounds.length) bounds = Arrays.copyOf(bounds, Math.max(newSize, bounds.length * 2));
            System.arraycopy(bounds, end, bounds, first + 2, size - end);
            bounds[first] = mergedMin;
            bounds[first + 1] = mergedMax;
            size = newSize;
        }
    }
}
