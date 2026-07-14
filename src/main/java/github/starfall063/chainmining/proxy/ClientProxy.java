package github.starfall063.chainmining.proxy;

import github.starfall063.chainmining.client.ChainMiningKeyBindings;
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
        ClientRegistry.registerKeyBinding(ChainMiningKeyBindings.CHAIN_MINING_KEY);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }
}
