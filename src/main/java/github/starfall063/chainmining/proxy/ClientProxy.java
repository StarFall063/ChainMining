package github.starfall063.chainmining.proxy;

import github.starfall063.chainmining.client.ChainMiningClientSettings;
import github.starfall063.chainmining.client.ChainMiningInputHandler;
import github.starfall063.chainmining.client.ChainMiningKeyBindings;
import github.starfall063.chainmining.client.ChainMiningPreviewHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ChainMiningClientSettings.init(event.getModConfigurationDirectory());
        ClientRegistry.registerKeyBinding(ChainMiningKeyBindings.TOGGLE_CHAIN_MINING);
        MinecraftForge.EVENT_BUS.register(new ChainMiningInputHandler());
        MinecraftForge.EVENT_BUS.register(new ChainMiningPreviewHandler());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }
}
