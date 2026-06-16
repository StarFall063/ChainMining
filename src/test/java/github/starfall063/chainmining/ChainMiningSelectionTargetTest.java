package github.starfall063.chainmining;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainMiningSelectionTargetTest {
    @Test
    void exactSelectionTargetUsesExactRenderedAndHiddenCounts() {
        List<BlockPos> positions = Arrays.asList(
                new BlockPos(0, 64, 0),
                new BlockPos(1, 64, 0),
                new BlockPos(2, 64, 0),
                new BlockPos(3, 64, 0)
        );
        ChainMiningHooks.SelectionTarget target = new ChainMiningHooks.SelectionTarget(ChainMiningHooks.ChainAction.MINE, positions, 4, 0, true);

        assertEquals(positions.subList(0, 2), target.getRenderedPositions(2));
        assertEquals(2, target.getRenderedCount(2));
        assertEquals(2, target.getHiddenCount(2));
    }

    @Test
    void inexactSelectionTargetPreservesKnownHiddenLowerBound() {
        List<BlockPos> positions = Arrays.asList(
                new BlockPos(0, 64, 0),
                new BlockPos(1, 64, 0),
                new BlockPos(2, 64, 0)
        );
        ChainMiningHooks.SelectionTarget target = new ChainMiningHooks.SelectionTarget(ChainMiningHooks.ChainAction.MINE, positions, 3, 5, false);

        assertEquals(3, target.getRenderedCount(8));
        assertEquals(5, target.getHiddenCount(8));
    }
}
