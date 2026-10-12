package github.starfall063.chainmining.util;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class PreviewSyncState {
    private List<BlockPos> lastBlocks;
    private int lastRangeLimit;
    private int lastRenderLimit;
    private long[] positions = new long[0];

    public boolean update(List<BlockPos> blocks, int rangeLimit, int renderLimit) {
        renderLimit = Math.max(0, Math.min(renderLimit, 8192));
        if (lastBlocks != null && lastRangeLimit == rangeLimit && lastRenderLimit == renderLimit
                && lastBlocks.equals(blocks)) return false;

        lastBlocks = new ArrayList<>(blocks);
        lastRangeLimit = rangeLimit;
        lastRenderLimit = renderLimit;
        positions = new long[Math.min(blocks.size(), renderLimit)];
        for (int i = 0; i < positions.length; i++) positions[i] = blocks.get(i).toLong();
        return true;
    }

    public int getTotal() {
        return lastBlocks == null ? 0 : lastBlocks.size();
    }

    public long[] getPositions() {
        return positions;
    }

    public void invalidate() {
        lastBlocks = null;
        positions = new long[0];
    }
}
