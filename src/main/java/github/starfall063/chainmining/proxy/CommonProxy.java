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
                ChainMiningNetwork.Handler.class,
                ChainMiningNetwork.ChainMiningPacket.class,
                0,
                Side.SERVER
        );
    }

    public void postInit(FMLPostInitializationEvent event) {

    }
}
