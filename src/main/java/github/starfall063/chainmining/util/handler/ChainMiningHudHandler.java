package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.ChainMiningLang;
import github.starfall063.chainmining.util.BlockMatchMode;
import github.starfall063.chainmining.util.ChainMiningStateManager;
import github.starfall063.chainmining.util.ChainShapeMode;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ChainMiningHudHandler {
    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (!ChainMiningStateManager.isClientEnabled()) return;

        BlockMatchMode matchMode = BlockMatchMode.fromName(ChainMiningConfig.CLIENT.chainMiningMatchMode);
        ChainShapeMode shapeMode = ChainShapeMode.fromName(ChainMiningConfig.CLIENT.chainMiningShape);

        boolean left = !"top_right".equals(ChainMiningConfig.CLIENT.chainMiningHudPosition)
                && !"bottom_right".equals(ChainMiningConfig.CLIENT.chainMiningHudPosition);
        List<String> target = left ? event.getLeft() : event.getRight();

        target.add(ChainMiningLang.tr(matchMode.getTranslationKey()) + " / " + ChainMiningLang.tr(shapeMode.getTranslationKey()));

        if (!ChainMiningConfig.CLIENT.chainMiningEnablePreview) {
            target.add(ChainMiningLang.tr("overlay.chainmining.disable"));
        } else {
            target.add(ChainMiningLang.tr("overlay.chainmining.info",
                    ChainMiningLang.tr(matchMode.getTranslationKey()),
                    ChainMiningConfig.CLIENT.chainMiningNeighborRange));
        }

        List<BlockPos> preview = ChainMiningStateManager.getPreviewBlocks();
        if (preview != null && !preview.isEmpty()) {
            int total = preview.size();
            int rendered = ChainMiningStateManager.getPreviewRendered();
            int hidden = ChainMiningStateManager.getPreviewHidden();
            target.add(ChainMiningLang.tr("overlay.chainmining.count", total, rendered, hidden));
        }
    }
}
