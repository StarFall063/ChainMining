package github.starfall063.chainmining;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

final class ChainMiningSelectionContext {
    private final World world;
    private final EntityPlayer player;
    private final BlockPos origin;
    private final EnumFacing face;
    private final ItemStack held;
    private final ChainShapeMode shapeMode;
    private final BlockMatchMode matchMode;
    private final IBlockState originState;
    private final BlockIdentity identity;

    private ChainMiningSelectionContext(World world, EntityPlayer player, BlockPos origin, EnumFacing face,
                                        ItemStack held, ChainShapeMode shapeMode, BlockMatchMode matchMode,
                                        IBlockState originState, BlockIdentity identity) {
        this.world = world;
        this.player = player;
        this.origin = origin;
        this.face = face;
        this.held = held;
        this.shapeMode = shapeMode;
        this.matchMode = matchMode;
        this.originState = originState;
        this.identity = identity;
    }

    static ChainMiningSelectionContext create(World world, EntityPlayer player, BlockPos origin, EnumFacing face,
                                              ItemStack held, ChainShapeMode shapeMode, BlockMatchMode matchMode,
                                              ChainMiningHooks.ChainAction action) {
        if (world == null || origin == null) {
            return null;
        }
        IBlockState originState = world.getBlockState(origin);
        if (originState == null) {
            return null;
        }
        ItemStack effectiveHeld = held == null ? ItemStack.EMPTY : held;
        ChainShapeMode effectiveShape = shapeMode == null ? ChainShapeMode.SHAPELESS : shapeMode;
        BlockMatchMode effectiveMatchMode = matchMode == null ? BlockMatchMode.META_ONLY : matchMode;
        EnumFacing effectiveFace = face == null ? EnumFacing.UP : face;
        ChainMiningHooks.ChainAction effectiveAction = action == null ? ChainMiningHooks.ChainAction.MINE : action;

        if (player != null && !ChainMiningHooks.canActOn(world, origin, originState, effectiveFace, player, effectiveHeld, effectiveAction)) {
            return null;
        }

        BlockIdentity identity = null;
        if (effectiveAction != ChainMiningHooks.ChainAction.SOW) {
            identity = BlockIdentity.from(world, origin, originState, effectiveMatchMode);
        }

        return new ChainMiningSelectionContext(world, player, origin, effectiveFace,
                effectiveHeld, effectiveShape, effectiveMatchMode, originState, identity);
    }

    World getWorld() { return this.world; }
    EntityPlayer getPlayer() { return this.player; }
    BlockPos getOrigin() { return this.origin; }
    EnumFacing getFace() { return this.face; }
    ItemStack getHeld() { return this.held; }
    ChainShapeMode getShapeMode() { return this.shapeMode; }
    BlockMatchMode getMatchMode() { return this.matchMode; }
    IBlockState getOriginState() { return this.originState; }
    BlockIdentity getIdentity() { return this.identity; }
}
