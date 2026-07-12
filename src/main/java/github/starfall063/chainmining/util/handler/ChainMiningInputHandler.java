package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.ChainMiningLang;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.client.ChainMiningKeyBindings;
import github.starfall063.chainmining.network.ChainMiningNetwork;
import github.starfall063.chainmining.util.BlockMatchMode;
import github.starfall063.chainmining.util.ChainMiningHooks;
import github.starfall063.chainmining.util.ChainMiningStateManager;
import github.starfall063.chainmining.util.ChainShapeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ChainMiningInputHandler {
    private static boolean lastEnabled;
    private static String lastShape = "";
    private static String lastMatchMode = "";
    private static int lastNeighborRange = -114514;
    private static int lastHitFace = -114514;
    private static long lastOrigin = Long.MIN_VALUE;
    private static boolean lastHasTarget;
    private static Item lastHeldItem;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            ChainMiningStateManager.setClientEnabled(false);
            return;
        }

        boolean keyDown = ChainMiningKeyBindings.CHAIN_MINING_KEY.isKeyDown();
        ChainMiningStateManager.setClientEnabled(keyDown);

        boolean hasTarget = false;
        long originPos = 0L;
        int face = -114514;
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK && mc.objectMouseOver.getBlockPos() != null) {
            hasTarget = true;
            originPos = mc.objectMouseOver.getBlockPos().toLong();
            face = mc.objectMouseOver.sideHit.ordinal();
        }
        syncIfChanged(keyDown, hasTarget, originPos, face);
    }

    @SubscribeEvent
    public static void onMouseEvent(MouseEvent event) {
        int wheel = event.getDwheel();
        if (wheel == 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (!ChainMiningStateManager.isClientEnabled()) return;
        if (!mc.gameSettings.keyBindSneak.isKeyDown()) return;

        if (GuiScreen.isCtrlKeyDown()) {
            cycleMatchMode(wheel);
        } else if (GuiScreen.isAltKeyDown()) {
            adjustNeighborRange(wheel);
        } else {
            cycleShape(wheel);
        }

        ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
        event.setCanceled(true);
    }

    private static void syncIfChanged(boolean enabled, boolean hasTarget, long origin, int face) {
        Minecraft mc =  Minecraft.getMinecraft();
        if (mc.getConnection() == null || mc.player == null) return;

        String shape = ChainMiningConfig.CLIENT.chainMiningShape;
        String matchMode = ChainMiningConfig.CLIENT.chainMiningMatchMode;
        int range = ChainMiningConfig.CLIENT.chainMiningNeighborRange;
        Item item = mc.player.getHeldItemMainhand().getItem();
        if (item != lastHeldItem) {
            ChainMiningStateManager.clearPreview();
        }

        if (enabled == lastEnabled && shape.equalsIgnoreCase(lastShape) && matchMode.equalsIgnoreCase(lastMatchMode) && range == lastNeighborRange && face == lastHitFace && hasTarget == lastHasTarget && origin == lastOrigin && item == lastHeldItem) return;

        lastEnabled = enabled;
        lastShape = shape;
        lastMatchMode = matchMode;
        lastNeighborRange = range;
        lastHitFace = face;
        lastHasTarget = hasTarget;
        lastOrigin = origin;
        lastHeldItem = item;

        ChainMiningNetwork.CHANNEL.sendToServer(new ChainMiningNetwork.StateMessage(enabled, shape, matchMode, range, face, hasTarget, origin));
    }

    private static void cycleMatchMode(int wheel) {
        BlockMatchMode current = BlockMatchMode.fromName(ChainMiningConfig.CLIENT.chainMiningMatchMode);
        BlockMatchMode next = wheel > 0 ? current.previous() : current.next();
        ChainMiningConfig.CLIENT.chainMiningMatchMode = next.getSerializedName();
        showStatus(ChainMiningLang.tr("message.chainmining.match_mode",
                ChainMiningLang.tr(next.getTranslationKey())));
    }

    private static void adjustNeighborRange(int wheel) {
        int current = ChainMiningConfig.CLIENT.chainMiningNeighborRange;
        int next = wheel > 0 ? current - 1 : current + 1;
        if (next < 1) next = 5;
        if (next > 5) next = 1;
        ChainMiningConfig.CLIENT.chainMiningNeighborRange = next;
        showStatus(ChainMiningLang.tr("message.chainmining.range", next));
    }

    private static void cycleShape(int wheel) {
        ChainShapeMode current = ChainShapeMode.fromName(ChainMiningConfig.CLIENT.chainMiningShape);
        ChainShapeMode next = wheel > 0 ? current.previous() : current.next();
        ChainMiningConfig.CLIENT.chainMiningShape = next.getSerializedName();
        showStatus(ChainMiningLang.tr("message.chainmining.shape",
                ChainMiningLang.tr(next.getTranslationKey())));
    }

    private static void showStatus(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            mc.player.sendStatusMessage(new TextComponentString(message), true);
        }
    }
}
