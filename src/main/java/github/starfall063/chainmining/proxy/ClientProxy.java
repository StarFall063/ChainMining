package github.starfall063.chainmining.proxy;

import github.starfall063.chainmining.client.ChainMiningKeyBindings;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.compact.TConstructCompact;
import github.starfall063.chainmining.item.ItemChainStorageTerminal;
import github.starfall063.chainmining.item.ItemMiningModifier;
import github.starfall063.chainmining.registry.ModRegistration;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (ModRegistration.isMiningModifierEnabled()) {
            ItemMiningModifier.INSTANCE.registerModels();
        }
        if (ModRegistration.isStorageTerminalEnabled()) {
            ItemChainStorageTerminal.INSTANCE.registerModels();
        }
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientRegistry.registerKeyBinding(ChainMiningKeyBindings.CHAIN_MINING_KEY);
        if (Loader.isModLoaded("tconstruct")) {
            TConstructCompact.registerBookPage();
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void registerModel(Item item, int meta, String id) {
        ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), id));
    }
}
