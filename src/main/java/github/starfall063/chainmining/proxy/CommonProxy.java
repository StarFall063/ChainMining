package github.starfall063.chainmining.proxy;

import github.starfall063.chainmining.network.ChainMiningNetwork;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {

    }

    public void init(FMLInitializationEvent event) {
        ChainMiningNetwork.CHANNEL.registerMessage(
                ChainMiningNetwork.StateMessage.Handler.class,
                ChainMiningNetwork.StateMessage.class, 0, Side.SERVER);
        ChainMiningNetwork.CHANNEL.registerMessage(
                ChainMiningNetwork.ConfigMessage.Handler.class,
                ChainMiningNetwork.ConfigMessage.class, 1, Side.CLIENT);
        ChainMiningNetwork.CHANNEL.registerMessage(
                ChainMiningNetwork.PreviewMessage.Handler.class,
                ChainMiningNetwork.PreviewMessage.class, 2, Side.CLIENT);
    }

    public void postInit(FMLPostInitializationEvent event) {

    }
}
