package github.starfall063.chainmining.util;

public class ChainMiningPreviewBudget {
    private final int renderLimit;
    private int rendered;
    private int totalScanned;

    public ChainMiningPreviewBudget(int renderLimit) {
        this.renderLimit = renderLimit;
    }

    public boolean canRender() {
        return rendered < renderLimit;
    }

    public void markRendered() {
        rendered++;
    }

    public int getRendered() {
        return rendered;
    }

    public int getTotalScanned() {
        return totalScanned;
    }

    public void setTotalScanned(int num) {
        this.totalScanned = num;
    }

    public int getHiddenCount() {
        return Math.max(0, totalScanned - rendered);
    }
}
