package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.ChainMiningLang;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.client.ChainMiningKeyBindings;
import github.starfall063.chainmining.util.BlockMatchMode;
import github.starfall063.chainmining.util.ChainMiningStateManager;
import github.starfall063.chainmining.util.ChainShapeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase !=TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            ChainMiningStateManager.setEnabled(false);
            return;
        }

        boolean keyDown = ChainMiningKeyBindings.CHAIN_MINING_KEY.isKeyDown();
        if (!mc.player.capabilities.isCreativeMode && keyDown && !ChainMiningConfig.SERVER.chainMiningIgnoreHeldItem) {
            ItemStack tool = mc.player.getHeldItemMainhand();
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = mc.objectMouseOver.getBlockPos();
                IBlockState state = mc.world.getBlockState(pos);
                String harvestTool = state.getBlock().getHarvestTool(state);
                if (harvestTool != null && (tool.isEmpty() || !tool.canHarvestBlock(state))) {
                    keyDown = false;
                }
            }
        }
        ChainMiningStateManager.setEnabled(keyDown);
    }

    @SubscribeEvent
    public static void onMouseEvent(MouseEvent event) {
        int wheel = event.getDwheel();
        if (wheel == 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (!ChainMiningStateManager.isEnabled()) return;
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

    private static void cycleMatchMode(int wheel) {
        BlockMatchMode current = BlockMatchMode.fromName(ChainMiningConfig.CLIENT.chainMiningMatchMode);
        BlockMatchMode next = wheel > 0 ? current.previous() : current.next();
        ChainMiningConfig.CLIENT.chainMiningMatchMode = next.getSerializedName();
        showStatus(ChainMiningLang.tr("message.chainmining.match_mode",
                ChainMiningLang.tr(next.getTranslationKey())));
    }

    private static void adjustNeighborRange(int wheel) {
        int current = ChainMiningConfig.SERVER.chainMiningNeighborRange;
        int next = wheel > 0 ? current - 1 : current + 1;
        if (next < 1) next = 5;
        if (next > 5) next = 1;
        ChainMiningConfig.SERVER.chainMiningNeighborRange = next;
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
