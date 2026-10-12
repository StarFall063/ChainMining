package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.util.ChainMiningHooks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Session {
        final BlockPos origin;
        final Set<BlockPos> area;
        final Map<ChainMiningHooks.ItemKey, Integer> drops = new LinkedHashMap<>();
        int xp = 0;

        Session(BlockPos origin, Set<BlockPos> area) {
            this.origin = origin;
            this.area = area;
        }

        void addDrop(ItemStack stack) {
            if (stack.isEmpty()) return;
            drops.merge(new ChainMiningHooks.ItemKey(stack), stack.getCount(), Integer::sum);
        }
    }
