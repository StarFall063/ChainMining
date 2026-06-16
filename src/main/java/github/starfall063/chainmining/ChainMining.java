package github.starfall063.chainmining;

import github.starfall063.chainmining.proxy.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ChainMining.MOD_ID, name = ChainMining.MOD_NAME, version = ChainMining.VERSION, guiFactory = "github.starfall063.chainmining.config.ChainMiningGuiFactory")
public class ChainMining {
    public static final String MOD_ID = "chainmining";
    public static final String MOD_NAME = "Chain Mining";
    public static final String VERSION = "1.0.0";

    public static final Logger logger = LogManager.getLogger(MOD_NAME);

    @SidedProxy(
            clientSide = "github.starfall063.chainmining.proxy.ClientProxy",
            serverSide = "github.starfall063.chainmining.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }
}
