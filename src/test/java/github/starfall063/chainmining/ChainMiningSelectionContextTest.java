package github.starfall063.chainmining;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class ChainMiningSelectionContextTest {
    @Test
    void createReturnsNullWhenRequiredInputsAreMissing() {
        assertNull(ChainMiningSelectionContext.create(null, null, BlockPos.ORIGIN, EnumFacing.UP, ItemStack.EMPTY, ChainShapeMode.SHAPELESS, BlockMatchMode.META_ONLY, ChainMiningHooks.ChainAction.MINE));
        assertNull(ChainMiningSelectionContext.create(null, null, null, EnumFacing.UP, ItemStack.EMPTY, ChainShapeMode.SHAPELESS, BlockMatchMode.META_ONLY, ChainMiningHooks.ChainAction.MINE));
        assertNull(ChainMiningSelectionContext.create(null, null, BlockPos.ORIGIN, null, ItemStack.EMPTY, ChainShapeMode.SHAPELESS, BlockMatchMode.META_ONLY, ChainMiningHooks.ChainAction.MINE));
        assertNull(ChainMiningSelectionContext.create(null, null, BlockPos.ORIGIN, EnumFacing.UP, ItemStack.EMPTY, ChainShapeMode.SHAPELESS, BlockMatchMode.META_ONLY, null));
    }
}
