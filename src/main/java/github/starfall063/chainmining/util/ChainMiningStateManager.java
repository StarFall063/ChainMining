package github.starfall063.chainmining.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public final class ChainMiningStateManager {
    private static volatile ChainMiningSelectionContext currentContext;
    private static volatile boolean enabled;
    private static volatile List<BlockPos> previewBlocks;
    private static volatile EnumFacing hitFace;
    private static volatile int previewTotal;
    private static volatile int previewRendered;
    private static volatile int previewHidden;

    private ChainMiningStateManager() {}

    public static EnumFacing getHitFace() {
        return hitFace;
    }

    public static void setHitFace(EnumFacing face) {
        hitFace = face;
    }

    public static List<BlockPos> getPreviewBlocks() {
        return previewBlocks;
    }

    public static void setPreviewBlocks(List<BlockPos> blocks) {
        previewBlocks = blocks;
    }

    public static void clearPreview() {
        previewBlocks = null;
        previewTotal = 0;
        previewRendered = 0;
        previewHidden = 0;
    }

    public static int getPreviewTotal() { return previewTotal; }
    public static void setPreviewTotal(int n) { previewTotal = n; }
    public static int getPreviewRendered() { return previewRendered; }
    public static void setPreviewRendered(int n) { previewRendered = n; }
    public static int getPreviewHidden() { return previewHidden; }
    public static void setPreviewHidden(int n) { previewHidden = n; }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        ChainMiningStateManager.enabled = enabled;
    }

    public static void setContext(ChainMiningSelectionContext context) {
        currentContext = context;
    }

    public static ChainMiningSelectionContext getContext() {
        return currentContext;
    }

    public static void clearContext() {
        currentContext = null;
    }

    public static boolean hasContext() {
        return currentContext != null;
    }
}
