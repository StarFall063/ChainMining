package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.util.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class ChainMiningEventHandler {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!ChainMiningStateManager.isEnabled()) return;

        BlockPos pos = event.getPos();
        IBlockState state = event.getState();
        BlockMatchMode mode = BlockMatchMode.fromName(ChainMiningConfig.CLIENT.chainMiningMatchMode);

        EntityPlayer player = event.getPlayer();
        ItemStack tool = player.getHeldItemMainhand();
        if (!player.capabilities.isCreativeMode && !ChainMiningConfig.SERVER.chainMiningIgnoreHeldItem) {
            String harvestTool = state.getBlock().getHarvestTool(state);
            if (harvestTool != null && (tool.isEmpty() || !tool.canHarvestBlock(state))) return;
            if (ChainMiningHooks.isToolBlacklisted(tool)) return;
        }

        if (ChainMiningHooks.isBlockBlacklisted(player.world, pos, state)) return;

        if (!player.capabilities.isCreativeMode) {
            if (player.getFoodStats().getFoodLevel() < ChainMiningConfig.SERVER.chainMiningMinFoodLevel) return;
        }

        BlockIdentity sourceId = BlockIdentity.from(player.world, pos, state, mode);
        if (sourceId == null) return;

        ChainShapeMode shapeMode = ChainShapeMode.fromName(ChainMiningConfig.CLIENT.chainMiningShape);
        EnumFacing hitFace = ChainMiningStateManager.getHitFace();
        List<BlockPos> blocks = ChainMiningHooks.scanBlocks(
                player.world, pos, state, sourceId, mode,
                ChainMiningConfig.SERVER.chainMiningMaxBlocks,
                ChainMiningConfig.SERVER.chainMiningNeighborRange,
                player,
                shapeMode,
                hitFace
        );
        ChainMiningHooks.executeChainMining(player, blocks, tool);
    }
}
