package github.starfall063.chainmining.util;

import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.server.ChainMiningServerPreview;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ChainMiningServerPreviewTick {
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        int interval = ChainMiningConfig.SERVER.chainMiningPreviewRefreshInterval;
        boolean forced = (interval > 0 && ++tickCounter % interval == 0);

        for (EntityPlayerMP playerMP : server.getPlayerList().getPlayers()) {
            UUID id = playerMP.getUniqueID();
            ChainMiningStateManager.PlayerState state = ChainMiningStateManager.getServerState(id);

            if (!state.enabled || !state.hasTarget) {
                if (state.dirty) {
                    ChainMiningServerPreview.recompute(playerMP);
                    state.dirty = false;
                }
                continue;
            }

            if (state.dirty || forced) {
                state.dirty = false;
                ChainMiningServerPreview.recompute(playerMP);
            }
        }
    }
}
