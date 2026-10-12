package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.ChainMiningLang;
import github.starfall063.chainmining.util.BlockMatchMode;
import github.starfall063.chainmining.util.ChainMiningStateManager;
import github.starfall063.chainmining.util.ChainShapeMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ChainMiningHudHandler {
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (!ChainMiningStateManager.isClientEnabled()) return;
        if (!ChainMiningConfig.CLIENT.chainMiningEnableHud) return;

        BlockMatchMode matchMode = BlockMatchMode.fromName(ChainMiningConfig.CLIENT.chainMiningMatchMode);
        ChainShapeMode shapeMode = ChainShapeMode.fromName(ChainMiningConfig.CLIENT.chainMiningShape);

        List<BlockPos> preview = ChainMiningStateManager.getPreviewBlocks();
        boolean hasTarget = mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK;

        List<String> lines = new ArrayList<>();
        lines.add(ChainMiningLang.tr("overlay.chainmining.status",
                ChainMiningLang.tr(ChainMiningConfig.CLIENT.chainMiningEnablePreview
                        ? "overlay.chainmining.preview_enabled"
                        : "overlay.chainmining.preview_disabled")));
        lines.add(ChainMiningLang.tr("overlay.chainmining.settings",
                ChainMiningLang.tr(shapeMode.getTranslationKey()),
                ChainMiningLang.tr(matchMode.getTranslationKey()),
                ChainMiningInputHandler.getCurrentNeighborRange(),
                ChainMiningStateManager.getServerNeighborRangeLimit()));

        if (hasTarget) {
            int total = ChainMiningStateManager.getPreviewTotal();
            if (preview != null && total <= 0) total = preview.size();
            int rendered = ChainMiningConfig.CLIENT.chainMiningEnablePreview
                    ? ChainMiningStateManager.getPreviewRendered() : 0;
            int hidden = ChainMiningConfig.CLIENT.chainMiningEnablePreview
                    ? ChainMiningStateManager.getPreviewHidden() : 0;
            lines.add(ChainMiningLang.tr("overlay.chainmining.count", total,
                    rendered, hidden));
        }

        drawHud(mc, event, lines, ChainMiningConfig.CLIENT.chainMiningHudPosition);
    }

    private static void drawHud(Minecraft mc, RenderGameOverlayEvent.Text event, List<String> lines, String position) {
        String normalized = position == null ? "" : position.trim().toLowerCase(java.util.Locale.ROOT);
        boolean centerX = normalized.endsWith("_center");
        boolean right = normalized.endsWith("_right");
        boolean middle = normalized.startsWith("middle_");
        boolean bottom = normalized.startsWith("bottom_");
        if (!(normalized.equals("top_left") || normalized.equals("top_center") || normalized.equals("top_right")
                || normalized.equals("middle_left") || normalized.equals("middle_right")
                || normalized.equals("bottom_left") || normalized.equals("bottom_center")
                || normalized.equals("bottom_right"))) {
            centerX = false;
            right = false;
            middle = false;
            bottom = false;
        }

        int screenWidth = event.getResolution().getScaledWidth();
        int screenHeight = event.getResolution().getScaledHeight();
        int lineHeight = mc.fontRenderer.FONT_HEIGHT;
        int blockHeight = lines.size() * lineHeight;
        int y = middle ? (screenHeight - blockHeight) / 2 : bottom ? screenHeight - 2 - blockHeight : 2;

        for (String line : lines) {
            int width = mc.fontRenderer.getStringWidth(line);
            int x = centerX ? (screenWidth - width) / 2 : right ? screenWidth - 2 - width : 2;
            Gui.drawRect(x - 1, y - 1, x + width + 1, y + lineHeight - 1, -1873784752);
            mc.fontRenderer.drawString(line, x, y, 14737632);
            y += lineHeight;
        }
    }
}
