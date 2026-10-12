package github.starfall063.chainmining.integration.ae2;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.ILocatable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.helpers.PlayerSource;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import java.util.function.UnaryOperator;

public final class AeSecurityStorage {
    private AeSecurityStorage() {
    }

    public static UnaryOperator<ItemStack> open(EntityPlayerMP player, String securityKey) {
        if (player == null || securityKey == null || securityKey.isEmpty() || !Loader.isModLoaded("appliedenergistics2")) return null;
        try {
            long serial = Long.parseLong(securityKey);
            ILocatable locatable = AEApi.instance().registries().locatable().getLocatableBy(serial);
            if (!(locatable instanceof IActionHost)) return null;

            IActionHost host = (IActionHost) locatable;
            IGridNode node = host.getActionableNode();
            if (node == null || node.getGrid() == null) return null;

            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
            if (storage == null || energy == null || !energy.isNetworkPowered()) return null;

            IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> inventory = storage.getInventory(channel);
            if (inventory == null) return null;
            return new Inserter(host, grid, energy, inventory, channel, new PlayerSource(player, host));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static ItemStack insert(EntityPlayerMP player, ItemStack input, String securityKey) {
        if (input.isEmpty()) return input;
        UnaryOperator<ItemStack> inserter = open(player, securityKey);
        return inserter == null ? input : inserter.apply(input);
    }

    private static final class Inserter implements UnaryOperator<ItemStack> {
        private final IActionHost host;
        private final IGrid grid;
        private final IEnergyGrid energy;
        private final IMEMonitor<IAEItemStack> inventory;
        private final IItemStorageChannel channel;
        private final PlayerSource source;

        private Inserter(IActionHost host, IGrid grid, IEnergyGrid energy, IMEMonitor<IAEItemStack> inventory,
                         IItemStorageChannel channel, PlayerSource source) {
            this.host = host;
            this.grid = grid;
            this.energy = energy;
            this.inventory = inventory;
            this.channel = channel;
            this.source = source;
        }

        @Override
        public ItemStack apply(ItemStack input) {
            if (input.isEmpty()) return input;
            try {
                IGridNode node = host.getActionableNode();
                if (node == null || node.getGrid() != grid || !energy.isNetworkPowered()) return input;
                IAEItemStack aeStack = channel.createStack(input);
                if (aeStack == null) return input;
                IAEItemStack remainder = AEApi.instance().storage().poweredInsert(
                        energy, inventory, aeStack, source, Actionable.MODULATE);
                return remainder == null ? ItemStack.EMPTY : remainder.createItemStack();
            } catch (RuntimeException ignored) {
                return input;
            }
        }
    }
}
