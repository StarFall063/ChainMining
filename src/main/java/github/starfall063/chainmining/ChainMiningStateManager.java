package github.starfall063.chainmining;

import github.starfall063.chainmining.client.ChainMiningClientSettings;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChainMiningStateManager {
    private static final Map<UUID, PlayerState> SERVER_STATES = new ConcurrentHashMap<>();
    private static volatile int syncedMaxBlocks = -1;
    private static volatile int syncedPreviewRenderLimit = -1;
    private static volatile int syncedDirectionalRange = -1;
    private static volatile boolean syncedIgnoreHeldItem;

    private ChainMiningStateManager() {
    }

    public static PlayerState getServerState(EntityPlayer player) {
        if (player == null) {
            return PlayerState.disabled();
        }
        return SERVER_STATES.getOrDefault(player.getUniqueID(), PlayerState.disabled());
    }

    public static void updateServerState(UUID playerId, boolean enabled, ChainShapeMode shapeMode, BlockMatchMode matchMode, int neighborRange) {
        if (playerId == null) {
            return;
        }
        SERVER_STATES.put(playerId, new PlayerState(enabled,
                shapeMode == null ? ChainShapeMode.SHAPELESS : shapeMode,
                matchMode == null ? BlockMatchMode.META_ONLY : matchMode,
                Math.max(1, Math.min(5, neighborRange))));
    }

    private static final Map<UUID, Long> LAST_STATE_UPDATE = new ConcurrentHashMap<>();
    private static final long STATE_UPDATE_COOLDOWN_MS = 250L;

    public static boolean isStateUpdateAllowed(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_STATE_UPDATE.get(playerId);
        if (last != null && now - last < STATE_UPDATE_COOLDOWN_MS) {
            return false;
        }
        LAST_STATE_UPDATE.put(playerId, now);
        return true;
    }

    public static void clearServerState(UUID playerId) {
        if (playerId != null) {
            SERVER_STATES.remove(playerId);
        }
    }

    public static boolean isClientEnabled() {
        return ChainMiningClientSettings.chainMiningEnabled;
    }

    public static void setClientEnabled(boolean enabled) {
        ChainMiningClientSettings.chainMiningEnabled = enabled;
    }

    public static ChainShapeMode getClientShape() {
        return ChainMiningClientSettings.chainMiningShape;
    }

    public static void setClientShape(ChainShapeMode shapeMode) {
        ChainMiningClientSettings.chainMiningShape = shapeMode == null ? ChainShapeMode.SHAPELESS : shapeMode;
    }

    public static BlockMatchMode getClientMatchMode() {
        return ChainMiningClientSettings.chainMiningMatchMode;
    }

    public static void setClientMatchMode(BlockMatchMode mode) {
        ChainMiningClientSettings.chainMiningMatchMode = mode == null ? BlockMatchMode.META_ONLY : mode;
    }

    private static volatile int clientNeighborRange = -1;

    public static int getClientNeighborRange() {
        return clientNeighborRange;
    }

    public static void setClientNeighborRange(int range) {
        clientNeighborRange = Math.max(1, Math.min(5, range));
    }

    public static int getEffectiveMaxBlocks() {
        return syncedMaxBlocks > 0 ? syncedMaxBlocks : ChainMiningConfig.maxBlocks;
    }

    public static int getEffectivePreviewRenderLimit() {
        return syncedPreviewRenderLimit > 0 ? syncedPreviewRenderLimit : ChainMiningConfig.previewRenderLimit;
    }

    public static int getEffectiveNeighborRange() {
        if (clientNeighborRange > 0) {
            return clientNeighborRange;
        }
        return 1;
    }

    public static int getEffectiveDirectionalRange() {
        return syncedDirectionalRange > 0 ? syncedDirectionalRange : ChainMiningConfig.directionalRange;
    }

    public static boolean getEffectiveIgnoreHeldItem() {
        return syncedIgnoreHeldItem;
    }

    public static void applySyncedConfig(int maxBlocks, int previewRenderLimit, int directionalRange, boolean ignoreHeldItem) {
        syncedMaxBlocks = ChainMiningConfig.clampMaxBlocks(maxBlocks);
        syncedPreviewRenderLimit = ChainMiningConfig.clampPreviewRenderLimit(previewRenderLimit);
        syncedDirectionalRange = ChainMiningConfig.clampDirectionalRange(directionalRange);
        syncedIgnoreHeldItem = ignoreHeldItem;
    }

    public static void clearSyncedConfig() {
        syncedMaxBlocks = -1;
        syncedPreviewRenderLimit = -1;
        syncedDirectionalRange = -1;
        syncedIgnoreHeldItem = false;
    }

    public static final class PlayerState {
        private static final PlayerState DISABLED = new PlayerState(false, ChainShapeMode.SHAPELESS, BlockMatchMode.META_ONLY, 1);
        private final boolean enabled;
        private final ChainShapeMode shapeMode;
        private final BlockMatchMode matchMode;
        private final int neighborRange;

        public PlayerState(boolean enabled, ChainShapeMode shapeMode, BlockMatchMode matchMode, int neighborRange) {
            this.enabled = enabled;
            this.shapeMode = shapeMode == null ? ChainShapeMode.SHAPELESS : shapeMode;
            this.matchMode = matchMode == null ? BlockMatchMode.META_ONLY : matchMode;
            this.neighborRange = Math.max(1, Math.min(5, neighborRange));
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public ChainShapeMode getShapeMode() {
            return this.shapeMode;
        }

        public BlockMatchMode getMatchMode() {
            return this.matchMode;
        }

        public int getNeighborRange() {
            return this.neighborRange;
        }

        public static PlayerState disabled() {
            return DISABLED;
        }
    }
}
