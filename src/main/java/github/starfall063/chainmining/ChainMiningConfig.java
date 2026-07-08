package github.starfall063.chainmining;

import com.cleanroommc.configanytime.ConfigAnytime;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
@Config(modid = Tags.MOD_ID, name = Tags.MOD_NAME)
public class ChainMiningConfig {
    @Config.Name("Client")
    public static final Client CLIENT = new Client();

    @Config.Name("Server")
    public static final Server SERVER = new Server();

    public static class Client {
        @Config.Name("ChainMiningShape")
        @Config.Comment({"Chain mining shape: SHAPELESS, PLANE, TUNNEL", "连锁形状: 无形状, 单向, 隧道"})
        public String chainMiningShape = "SHAPELESS";

        @Config.Name("ChainMiningMatchMode")
        @Config.Comment({"Block match mode: META_ONLY, NBT_ONLY, REGISTRY_ONLY, NBT_META", "方块匹配模式: 仅meta, 仅nbt, 仅注册名, meta和nbt"})
        public String chainMiningMatchMode = "NBT_META";

        @Config.Name("ChainMiningHudPosition")
        @Config.Comment({"HUD overlay position: top_left, top_right, bottom_left, bottom_right", "HUD位置: 左上, 右上, 左下, 右下"})
        public String chainMiningHudPosition = "top_left";

        @Config.Name("ChainMiningPreviewColor")
        @Config.Comment({"Preview wireframe color in hex ARGB (e.g. FFE65CEB)", "预览边框颜色(十六进制ARGB, 例如: FFE65CEB)"})
        public String chainMiningPreviewColor = "FFE65CEB";
    }

    public static class Server {
        @Config.Name("ChainMiningMaxBlocks")
        @Config.Comment({"Maximum number of chain mining at once", "单次连锁的最大数量"})
        @Config.RangeInt(min = 1, max = 1024)
        public int chainMiningMaxBlocks = 128;

        @Config.Name("ChainMiningPreviewRenderLimit")
        @Config.Comment({"Maximum number of renders for a single chain mining preview", "单次连锁预览的最大渲染数量"})
        @Config.RangeInt(min = 1, max = 1024)
        public int chainMiningPreviewRenderLimit = 128;

        @Config.Name("ChainMiningNeighborRange")
        @Config.Comment({"Shapeless chain mining neighbor search radius (5 = up to 5 blocks away)", "连锁挖掘相邻搜索半径 (5=最多5格远)"})
        @Config.RangeInt(min = 1, max = 5)
        public int chainMiningNeighborRange = 1;

        @Config.Name("ChainMiningExhaustionPerBlock")
        @Config.Comment({"Exhaustion added for each block mined by chain mining", "连锁每个方块消耗的饥饿值"})
        @Config.RangeDouble(min = 0.0D, max = 20.0D)
        public double chainMiningExhaustionPerBlock = 0.1;

        @Config.Name("ChainMiningMinFoodLevel")
        @Config.Comment({"Minimum food level required to start or continue survival-mode chain mining.", "生存模式下继续或触发连锁需要的最低饥饿值"})
        @Config.RangeDouble(min = 0.0, max = 20.0)
        public double chainMiningMinFoodLevel = 0.1;

        @Config.Name("ChainMiningIgnoreHeldItem")
        @Config.Comment({"When enabled, chain mining activates regardless of the item held in hand, ignore the tool blacklist.", "启用时, 无论是否手持物品或工具都可进行连锁, 无视工具黑名单"})
        public boolean chainMiningIgnoreHeldItem = false;

        @Config.Name("ChainMiningToolBlackList")
        @Config.Comment({"Items that will NOT trigger chain mining. (e.g. minecraft:diamond:pickaxe)", "不会触发连锁的物品列表 (例如: minecraft:diamond:pickaxe)"})
        public String[] chainMiningToolBlackList = {};

        @Config.Name("ChainMiningBlockBlackList")
        @Config.Comment({"Blocks that will NOT be chain mined. (e.g. minecraft:stone:0)", "连锁黑名单(例: minecraft:stone:0)"})
        public String[] chainMiningBlockBlackList = {};

        @Config.Name("ChainMiningNbtMatchKeys")
        @Config.Comment({"Whitelist of tags on blocks matched when NBT matching is enabled", "启用nbt匹配时匹配的方块所带有的标签白名单"})
        public String[] chainMiningNbtMatchKeys = {
                "id",
                "tier",
                "type"
        };
    }

    static {
        ConfigAnytime.register(ChainMiningConfig.class);
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Tags.MOD_ID)) {
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }
}
