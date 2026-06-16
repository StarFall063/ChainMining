package github.starfall063.chainmining;

final class ChainMiningPreviewBudget {
    private final int renderLimit;
    private final int collectionLimit;

    private ChainMiningPreviewBudget(int renderLimit, int collectionLimit) {
        this.renderLimit = renderLimit;
        this.collectionLimit = collectionLimit;
    }

    static ChainMiningPreviewBudget create(int previewRenderLimit, int maxBlocks) {
        int effectiveRenderLimit = Math.max(1, previewRenderLimit);
        int effectiveMaxBlocks = Math.max(1, maxBlocks);
        int collectionLimit = Math.min(effectiveMaxBlocks, Math.max(effectiveRenderLimit + 64, effectiveRenderLimit * 2));
        return new ChainMiningPreviewBudget(effectiveRenderLimit, collectionLimit);
    }

    int getRenderLimit() {
        return this.renderLimit;
    }

    int getCollectionLimit() {
        return this.collectionLimit;
    }
}
