package github.starfall063.chainmining.util.handler;

import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.util.ChainMiningStateManager;
import github.starfall063.chainmining.util.SneakGrowthHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class ChainMiningPlayerHandler {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        SneakGrowthHandler.onPlayerTick(event);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ChainMiningStateManager.clearServerState(event.player.getUniqueID());
        SneakGrowthHandler.clearPlayer(event.player.getUniqueID());
    }
}
