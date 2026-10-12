package github.starfall063.chainmining.registry;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.enchantment.CMEnchantment;
import github.starfall063.chainmining.item.ItemChainStorageTerminal;
import github.starfall063.chainmining.item.ItemMiningModifier;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModRegistration {
    private ModRegistration() {
    }

    public static boolean isStorageTerminalEnabled() {
        return Loader.isModLoaded("appliedenergistics2");
    }

    public static boolean isMiningModifierEnabled() {
        return Loader.isModLoaded("tconstruct") && ChainMiningConfig.SERVER.chainMiningRequireEnchantment;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (isMiningModifierEnabled()) event.getRegistry().register(ItemMiningModifier.INSTANCE);
        if (isStorageTerminalEnabled()) event.getRegistry().register(ItemChainStorageTerminal.INSTANCE);
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        if (ChainMiningConfig.SERVER.chainMiningRequireEnchantment) event.getRegistry().register(CMEnchantment.INSTANCE);
    }
}
