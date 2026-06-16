package github.starfall063.chainmining;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainMiningPreviewBudgetTest {
    @Test
    void createClampsRenderLimitAndExpandsCollectionWindow() {
        ChainMiningPreviewBudget budget = ChainMiningPreviewBudget.create(128, 512);

        assertEquals(128, budget.getRenderLimit());
        assertEquals(256, budget.getCollectionLimit());
    }

    @Test
    void createClampsZeroRenderLimitToOneAndRespectsMaxBlocks() {
        ChainMiningPreviewBudget budget = ChainMiningPreviewBudget.create(0, 80);

        assertEquals(1, budget.getRenderLimit());
        assertEquals(65, budget.getCollectionLimit());
    }
}
