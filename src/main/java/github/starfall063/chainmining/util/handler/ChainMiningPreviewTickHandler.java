package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ChainMiningPreviewTickHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) return;
        if (!ChainMiningStateManager.isEnabled()) {
            ChainMiningStateManager.clearPreview();
            return;
        }

        RayTraceResult target = mc.objectMouseOver;
        if (target == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            ChainMiningStateManager.clearPreview();
            return;
        }

        BlockPos pos = target.getBlockPos();
        if (pos == null) {
            ChainMiningStateManager.clearPreview();
            return;
        }

        BlockMatchMode matchMode = BlockMatchMode.fromName(ChainMiningConfig.CLIENT.chainMiningMatchMode);
        BlockIdentity sourceId = BlockIdentity.from(mc.world, pos, mc.world.getBlockState(pos), matchMode);
        if (sourceId == null) {
            ChainMiningStateManager.clearPreview();
            return;
        }

        ChainMiningStateManager.setHitFace(target.sideHit);
        ChainShapeMode shapeMode = ChainShapeMode.fromName(ChainMiningConfig.CLIENT.chainMiningShape);

        List<BlockPos> blocks = scanByShape(mc.world, pos, mc.world.getBlockState(pos), sourceId, matchMode, shapeMode);

        ChainMiningStateManager.setPreviewTotal(blocks.size());
        ChainMiningStateManager.setPreviewBlocks(blocks);
    }

    private static List<BlockPos> scanByShape(World world, BlockPos pos, IBlockState state, BlockIdentity sourceId, BlockMatchMode matchMode, ChainShapeMode shapeMode) {
        int max = ChainMiningConfig.SERVER.chainMiningMaxBlocks;
        int range = ChainMiningConfig.SERVER.chainMiningNeighborRange;
        EnumFacing hitFace = ChainMiningStateManager.getHitFace();
        Minecraft mc = Minecraft.getMinecraft();
        return ChainMiningHooks.scanBlocks(world, pos, state, sourceId, matchMode, max, range, mc.player, shapeMode, hitFace);
    }
}
