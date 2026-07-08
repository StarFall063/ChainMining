package github.starfall063.chainmining.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ChainMiningSelectionContext {
    private final List<BlockPos> blocks;
    private final BlockIdentity sourceBlock;
    private final ItemStack tool;
    private final BlockPos startPos;
    private final ChainMiningPreviewBudget budget;

    public ChainMiningSelectionContext(List<BlockPos> blocks, BlockIdentity sourceBlock, ItemStack tool, BlockPos startPos, ChainMiningPreviewBudget budget) {
        this.blocks = blocks;
        this.sourceBlock = sourceBlock;
        this.tool = tool;
        this.startPos = startPos;
        this.budget = budget;
    }

    public List<BlockPos> getBlocks() {
        return blocks;
    }

    public BlockIdentity getSourceBlock() {
        return sourceBlock;
    }

    public ItemStack getTool() {
        return tool;
    }

    public BlockPos getStartPos() {
        return startPos;
    }

    public ChainMiningPreviewBudget getBudget() {
        return this.budget;
    }
}
