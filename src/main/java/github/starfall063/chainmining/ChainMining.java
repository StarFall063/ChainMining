package github.starfall063.chainmining;

import github.starfall063.chainmining.compact.TConstructCompact;
import github.starfall063.chainmining.proxy.CommonProxy;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, dependencies = ChainMining.DEPENDENCIES)
public class ChainMining {
    public static final Logger logger = LogManager.getLogger(Tags.MOD_NAME);
    public static final String DEPENDENCIES = "required-after:configanytime;after:tconstruct;after:appliedenergistics2";
    public static final String CLIENT_PROXY = "github.starfall063.chainmining.proxy.ClientProxy";
    public static final String COMMON_PROXY = "github.starfall063.chainmining.proxy.CommonProxy";

    @SidedProxy(clientSide = CLIENT_PROXY, serverSide = COMMON_PROXY)
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        if (Loader.isModLoaded("tconstruct") && ChainMiningConfig.SERVER.chainMiningRequireEnchantment) {
            TConstructCompact.register();
        }
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
