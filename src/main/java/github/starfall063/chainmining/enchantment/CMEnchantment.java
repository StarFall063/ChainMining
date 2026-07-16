package github.starfall063.chainmining.enchantment;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.Tags;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class CMEnchantment {
    public static Enchantment INSTANCE = new ChainMiningEnchantment();

    @SubscribeEvent
    public static void registerEnchantment(RegistryEvent.Register<Enchantment> event) {
        if (ChainMiningConfig.SERVER.chainMiningRequireEnchantment) {
            registerEnchantment(event.getRegistry());
        }
    }

    public static void registerEnchantment(IForgeRegistry<Enchantment> registry)
    {
        registry.register(INSTANCE);
    }
}
