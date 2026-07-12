package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.network.ChainMiningNetwork;
import github.starfall063.chainmining.util.ChainMiningStateManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class ChainMiningPlayerHandler {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP)) return;
        ChainMiningNetwork.CHANNEL.sendTo(new ChainMiningNetwork.ConfigMessage(
                ChainMiningConfig.SERVER.chainMiningMaxBlocks,
                ChainMiningConfig.SERVER.chainMiningIgnoreHeldItem,
                ChainMiningConfig.SERVER.chainMiningToolBlackList,
                ChainMiningConfig.SERVER.chainMiningBlockBlackList),
                (EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ChainMiningStateManager.clearServerState(event.player.getUniqueID());
    }
}
