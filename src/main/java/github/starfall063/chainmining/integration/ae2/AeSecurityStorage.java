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

public final class AeSecurityStorage {
    private AeSecurityStorage() {
    }

    public static ItemStack insert(EntityPlayerMP player, ItemStack input, String securityKey) {
        if (input.isEmpty() || securityKey == null || securityKey.isEmpty() || !Loader.isModLoaded("appliedenergistics2")) return input;
        try {
            long serial = Long.parseLong(securityKey);
            ILocatable locatable = AEApi.instance().registries().locatable().getLocatableBy(serial);
            if (!(locatable instanceof IActionHost)) return input;

            IActionHost host = (IActionHost) locatable;
            IGridNode node = host.getActionableNode();
            if (node == null || node.getGrid() == null) return input;

            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IEnergyGrid energy = grid.getCache(IEnergyGrid.class);
            if (storage == null || energy == null || !energy.isNetworkPowered()) return input;

            IItemStorageChannel channel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
            IMEMonitor<IAEItemStack> inventory = storage.getInventory(channel);
            if (inventory == null) return input;
            IAEItemStack aeStack = channel.createStack(input);
            if (aeStack == null) return input;
            IAEItemStack remainder = AEApi.instance().storage().poweredInsert(energy, inventory, aeStack, new PlayerSource(player, host), Actionable.MODULATE);
            return remainder == null ? ItemStack.EMPTY : remainder.createItemStack();
        } catch (RuntimeException ignored) {
            return input;
        }
    }
}
