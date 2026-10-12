package github.starfall063.chainmining;

import com.cleanroommc.configanytime.ConfigAnytime;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
@Config(modid = Tags.MOD_ID, name = Tags.MOD_ID)
public class ChainMiningConfig {
    @Config.Name("Client")
    public static final Client CLIENT = new Client();

    @Config.Name("Server")
    public static final Server SERVER = new Server();

    public static int effectiveMaxBlocks() {
        return Math.max(1, Math.min(SERVER.chainMiningMaxBlocks, 8192));
    }

    public static int effectiveNeighborRange() {
        return Math.max(1, Math.min(SERVER.chainMiningNeighborRange, 16));
    }

    public static int effectivePreviewRefreshInterval() {
        return Math.max(0, Math.min(SERVER.chainMiningPreviewRefreshInterval, 20));
    }

    public static int effectivePreviewRenderLimit() {
        return Math.max(1, Math.min(CLIENT.chainMiningPreviewRenderLimit, 8192));
    }

    public static int effectiveMinFoodLevel() {
        return Math.max(0, Math.min(SERVER.chainMiningMinFoodLevel, 20));
    }

    public static double effectiveExhaustionPerBlock() {
        return Math.max(0.0D, Math.min(SERVER.chainMiningExhaustionPerBlock, 20.0D));
    }

    public static int effectiveSneakGrowRadius() {
        return Math.max(1, Math.min(SERVER.chainMiningSneakGrowRadius, 16));
    }

    static {
        ConfigAnytime.register(ChainMiningConfig.class);
    }

    public static class Client {
        @Config.Name("ChainMiningShape")
        @Config.Comment({"Chain shape: shapeless, plane, or 3x3 tunnel.", "连锁形状：无形状、单向或3*3隧道。"})
        public String chainMiningShape = "shapeless";

        @Config.Name("ChainMiningMatchMode")
        @Config.Comment({"Match mode: meta, nbt, nbt_meta, or registry-only.", "匹配模式：meta、nbt、nbt_meta或仅注册名。"})
        public String chainMiningMatchMode = "meta";

        @Config.Name("ChainMiningHudPosition")
        @Config.Comment({"HUD position: 8 presets, top/middle/bottom by left/center/right; invalid values use top_left.", "HUD位置：顶部/中部/底部×左/中/右共8种；无效值使用左上。"})
        public String chainMiningHudPosition = "top_left";

        @Config.Name("ChainMiningPreviewColor")
        @Config.Comment({"Preview wireframe color: 8-digit ARGB hexadecimal value or rainbow.", "预览线框颜色：8位ARGB十六进制值或rainbow。"})
        public String chainMiningPreviewColor = "FFE65CEB";

        @Config.Name("ChainMiningPreviewRenderLimit")
        @Config.Comment({"Maximum preview wireframe count; higher values affect performance.", "预览最大线框数量，数值越大越影响性能。"})
        @Config.RangeInt(min = 1, max = 8192)
        public int chainMiningPreviewRenderLimit = 128;

        @Config.Name("ChainMiningEnablePreview")
        @Config.Comment({"Whether to render the chain-mining preview wireframe.", "是否渲染连锁预览线框。"})
        public boolean chainMiningEnablePreview = true;

        @Config.Name("ChainMiningEnableHud")
        @Config.Comment({"Whether to display the chain-mining HUD.", "是否显示连锁HUD。"})
        public boolean chainMiningEnableHud = true;

    }

    public static class Server {
        @Config.Name("ChainMiningRequireEnchantment")
        @Config.Comment({"Survival mode requires the Chain Mining enchantment or the Tinkers' Antique modifier (requires restart).", "生存模式需要连锁附魔或匠魂怀古对应升级(需要重启游戏)。"})
        @Config.RequiresMcRestart
        public boolean chainMiningRequireEnchantment = false;

        @Config.Name("ChainMiningMaxBlocks")
        @Config.Comment({"Maximum number of blocks per chain operation.", "单次连锁最大方块数量。"})
        @Config.RangeInt(min = 1, max = 8192)
        public int chainMiningMaxBlocks = 128;

        @Config.Name("ChainMiningNeighborRange")
        @Config.Comment({"Maximum neighbor search range; clients can select from 1 to this limit.", "最大相邻搜索范围, 客户端可在1到该上限间选择。"})
        @Config.RangeInt(min = 1, max = 16)
        public int chainMiningNeighborRange = 5;

        @Config.Name("ChainMiningExhaustionPerBlock")
        @Config.Comment({"Exhaustion added per block during chain mining.", "连锁每个方块增加的疲惫值。"})
        @Config.RangeDouble(min = 0.0D, max = 20.0D)
        public double chainMiningExhaustionPerBlock = 0.1;

        @Config.Name("ChainMiningMinFoodLevel")
        @Config.Comment({"Minimum hunger level before each block is processed.", "每个方块执行前的最低饥饿值。"})
        @Config.RangeInt(min = 0, max = 20)
        public int chainMiningMinFoodLevel = 1;

        @Config.Name("ChainMiningIgnoreHeldItem")
        @Config.Comment({"Ignore held-item, blacklist, and enchantment checks.", "忽略手持物品、黑名单和附魔检查。"})
        public boolean chainMiningIgnoreHeldItem = false;

        @Config.Name("ChainMiningRefreshInterval")
        @Config.Comment({"Server preview refresh interval, in ticks (0=disabled, 1=every tick).", "服务端预览刷新间隔, 单位为tick（0关闭，1为每tick）。"})
        @Config.RangeInt(min = 0, max = 20)
        public int chainMiningPreviewRefreshInterval = 1;

        @Config.Name("ChainMiningToolBlackList")
        @Config.Comment({"Tool blacklist; supports metadata and wildcards.", "工具黑名单，支持metadata和通配符。"})
        public String[] chainMiningToolBlackList = {};

        @Config.Name("ChainMiningBlockBlackList")
        @Config.Comment({"Block blacklist; supports metadata and wildcards.", "方块黑名单，支持metadata和通配符。"})
        public String[] chainMiningBlockBlackList = {};

        @Config.Name("ChainMiningNbtMatchKeys")
        @Config.Comment({"NBT keys used when NBT matching is enabled.", "带有nbt匹配时使用的NBT键。"})
        public String[] chainMiningNbtMatchKeys = {
                "id",
                "tier",
                "type"
        };

        @Config.Name("ChainMiningEquivalentBlockGroups")
        @Config.Comment({"Equivalent block groups: each entry is a comma-separated group; supports wildcards and a trailing :* metadata wildcard.", "等价方块组：每项为逗号分隔的一组，支持通配符和末尾:*元数据通配。"})
        public String[] chainMiningEquivalentBlockGroups = {
                "minecraft:tallgrass:*,minecraft:yellow_flower:*,minecraft:red_flower:*,minecraft:double_plant:*"
        };

        @Config.Name("ChainMiningSneakGrowEnabled")
        @Config.Comment({"Whether to enable sneak growth.", "是否启用下蹲催熟。"})
        public boolean chainMiningSneakGrowEnabled = true;

        @Config.Name("ChainMiningSneakGrowWhitelist")
        @Config.Comment({"When non-empty, only matching growth blocks are allowed; supports wildcards.", "非空时仅允许匹配的催熟方块，支持通配符。"})
        public String[] chainMiningSneakGrowWhitelist = {};

        @Config.Name("ChainMiningSneakGrowBlacklist")
        @Config.Comment({"Growth blacklist; takes priority over the whitelist and supports wildcards.", "催熟黑名单，优先于白名单，支持通配符。"})
        public String[] chainMiningSneakGrowBlacklist = {
                "minecraft:grass",
                "minecraft:tallgrass"
        };

        @Config.Name("ChainMiningSneakGrowRadius")
        @Config.Comment({"Cubic radius for sneak growth (1-16).", "下蹲催熟的立方体半径（1-16）。"})
        @Config.RangeInt(min = 1, max = 16)
        public int chainMiningSneakGrowRadius = 3;

    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Tags.MOD_ID)) {
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        }
    }
}
