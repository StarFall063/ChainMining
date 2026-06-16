package github.starfall063.chainmining.proxy;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.ChainMiningEventHandler;
import github.starfall063.chainmining.network.ChainMiningNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {
        ChainMiningConfig.init(event.getModConfigurationDirectory());
        ChainMiningNetwork.init();
        MinecraftForge.EVENT_BUS.register(new ChainMiningConfig());
        MinecraftForge.EVENT_BUS.register(new ChainMiningEventHandler());
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
        if (ChainMiningConfig.CONFIG.hasChanged()) {
            ChainMiningConfig.CONFIG.save();
        }
    }
}
