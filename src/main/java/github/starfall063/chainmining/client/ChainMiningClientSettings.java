package github.starfall063.chainmining.client;

import github.starfall063.chainmining.BlockMatchMode;
import github.starfall063.chainmining.ChainShapeMode;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class ChainMiningClientSettings {
    public static boolean chainMiningEnabled = false;
    public static ChainShapeMode chainMiningShape = ChainShapeMode.SHAPELESS;
    public static BlockMatchMode chainMiningMatchMode = BlockMatchMode.META_ONLY;
    public static String chainMiningHudPosition = "top_left";
    public static int chainMiningPreviewColor = 0xFFE65CEB;
    private static Configuration configuration;

    private ChainMiningClientSettings() {
    }

    public static void init(File configDir) {
        File configFile = new File(configDir, "chainmining_client.cfg");
        configuration = new Configuration(configFile);
        load();
    }

    public static void load() {
        if (configuration == null) {
            return;
        }

        chainMiningEnabled = false;
        chainMiningShape = ChainShapeMode.fromName(configuration.getString(
                "chainMiningShape",
                "controls",
                chainMiningShape.getSerializedName(),
                "Chain mining shape: SHAPELESS, PLANE, TUNNEL"
        ));
        chainMiningMatchMode = BlockMatchMode.fromName(configuration.getString(
                "chainMiningMatchMode",
                "controls",
                chainMiningMatchMode.getSerializedName(),
                "Block match mode: meta, nbt, nbt_meta, registry"
        ));
        chainMiningHudPosition = configuration.getString(
                "chainMiningHudPosition",
                "controls",
                chainMiningHudPosition,
                "HUD overlay position: top_left, top_right, bottom_left, bottom_right"
        );
        chainMiningPreviewColor = Integer.parseUnsignedInt(configuration.getString(
                "chainMiningPreviewColor",
                "controls",
                Integer.toHexString(chainMiningPreviewColor),
                "Preview wireframe color in hex ARGB (e.g. FFE65CEB)"
        ), 16);

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    public static void save() {
        if (configuration == null) {
            return;
        }

        configuration.get("controls", "chainMiningShape", ChainShapeMode.SHAPELESS.getSerializedName()).set(chainMiningShape.getSerializedName());
        configuration.get("controls", "chainMiningMatchMode", BlockMatchMode.META_ONLY.getSerializedName()).set(chainMiningMatchMode.getSerializedName());
        configuration.get("controls", "chainMiningHudPosition", "top_left").set(chainMiningHudPosition);
        configuration.get("controls", "chainMiningPreviewColor", "FFE65CEB").set(Integer.toHexString(chainMiningPreviewColor));

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
