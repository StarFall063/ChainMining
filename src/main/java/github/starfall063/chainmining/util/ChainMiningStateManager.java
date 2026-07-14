package github.starfall063.chainmining.util;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChainMiningStateManager {
    private static volatile List<BlockPos> previewBlocks;
    private static volatile EnumFacing hitFace;
    private static volatile int previewTotal;
    private static volatile int previewRendered;
    private static volatile int previewHidden;
    private static volatile boolean clientEnabled;
    private static final Map<UUID, PlayerState> SERVER_STATES = new ConcurrentHashMap<>();

    public static boolean isClientEnabled() {
        return clientEnabled;
    }

    public static void setClientEnabled(boolean value) {
        clientEnabled = value;
    }

    public static PlayerState getServerState(UUID id) {
        return SERVER_STATES.computeIfAbsent(id, k -> new PlayerState());
    }

    public static boolean isServerEnabled(UUID id) {
        return SERVER_STATES.get(id) != null && SERVER_STATES.get(id).enabled;
    }

    public static void clearServerState(UUID id ) {
        SERVER_STATES.remove(id);
    }

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

    public static final class PlayerState {
        public boolean enabled;
        public String shape = "SHAPELESS";
        public String matchMode = "meta";
        public int neighborRange = 1;
        public EnumFacing hitFace = EnumFacing.UP;
        public boolean hasTarget;
        public long originPos;
        public boolean dirty;
    }
}
