package github.starfall063.chainmining.util.handler;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Session {
        final BlockPos origin;
        final Set<BlockPos> area;
        final List<ItemStack> drops = new ArrayList<>();
        int xp = 0;

        Session(BlockPos origin, Set<BlockPos> area) {
            this.origin = origin;
            this.area = area;
        }
    }
