package github.starfall063.chainmining;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

public final class ChainMiningConfig {
    public static Configuration CONFIG;
    public static final int MIN_MAX_BLOCKS = 1;
    public static final int MAX_MAX_BLOCKS = 1024;
    public static final int MIN_PREVIEW_RENDER_LIMIT = 1;
    public static final int MAX_PREVIEW_RENDER_LIMIT = 1024;
    public static final int MIN_DIRECTIONAL_RANGE = 8;
    public static final int MAX_DIRECTIONAL_RANGE = 64;

    public static int maxBlocks;
    public static int previewRenderLimit;
    public static int directionalRange;
    public static int minFoodLevel;
    public static float exhaustionPerBlock;
    public static boolean reachFilter;
    public static boolean ignoreHeldItem;

    public static void init(File file) {
        CONFIG = new Configuration(new File(file, "chainmining.cfg"));
        loadConfig();
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(ChainMining.MOD_ID)) {
            syncConfig();
        }
    }

    private static void loadConfig() {
        Configuration cfg = CONFIG;
        try {
            cfg.load();
            syncProperties(cfg);
        } catch (Exception e) {
            ChainMining.logger.error("Failed to load config file!", e);
        } finally {
            if (cfg.hasChanged()) {
                cfg.save();
            }
        }
    }

    private static void syncConfig() {
        syncProperties(CONFIG);
        CONFIG.save();
    }

    private static void syncProperties(Configuration cfg) {
        String category = "ChainMining";
        cfg.addCustomCategoryComment(category, "Chain mining settings");

        maxBlocks = clampMaxBlocks(cfg.getInt(
                "maxBlocks", category, 128, MIN_MAX_BLOCKS, MAX_MAX_BLOCKS,
                "Maximum number of blocks that chain mining may affect at once."
        ));
        previewRenderLimit = clampPreviewRenderLimit(cfg.getInt(
                "previewRenderLimit", category, 256, MIN_PREVIEW_RENDER_LIMIT, MAX_PREVIEW_RENDER_LIMIT,
                "Maximum number of blocks that chain mining preview may render at once."
        ));
        directionalRange = clampDirectionalRange(cfg.getInt(
                "directionalRange", category, 16, MIN_DIRECTIONAL_RANGE, MAX_DIRECTIONAL_RANGE,
                "Maximum extension length used by the facing-chain shape."
        ));
        if (cfg.hasKey(category, "requireTool")) {
            cfg.getCategory(category).remove("requireTool");
        }
        minFoodLevel = Math.max(0, Math.min(20, cfg.getInt(
                "minFoodLevel", category, 1, 0, 20,
                "Minimum food level required to start or continue survival-mode chain mining."
        )));
        exhaustionPerBlock = (float) Math.max(0.0D, cfg.getFloat(
                "exhaustionPerBlock", category, 0.25F, 0.0F, 20.0F,
                "Exhaustion added for each extra block mined by chain mining."
        ));
        reachFilter = cfg.getBoolean(
                "reachFilter", category, false,
                "When enabled, blocks beyond the player's reach distance are skipped during chain mining."
        );
        ignoreHeldItem = cfg.getBoolean(
                "ignoreHeldItem", category, false,
                "When enabled, chain mining activates regardless of the item held in hand."
        );
    }

    public static int clampMaxBlocks(int value) {
        return Math.max(MIN_MAX_BLOCKS, Math.min(MAX_MAX_BLOCKS, value));
    }

    public static int clampPreviewRenderLimit(int value) {
        return Math.max(MIN_PREVIEW_RENDER_LIMIT, Math.min(MAX_PREVIEW_RENDER_LIMIT, value));
    }

    public static int clampDirectionalRange(int value) {
        return Math.max(MIN_DIRECTIONAL_RANGE, Math.min(MAX_DIRECTIONAL_RANGE, value));
    }
}
