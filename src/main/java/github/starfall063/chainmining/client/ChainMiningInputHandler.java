package github.starfall063.chainmining.client;

import github.starfall063.chainmining.BlockMatchMode;
import github.starfall063.chainmining.ChainMiningHooks;
import github.starfall063.chainmining.ChainMiningLang;
import github.starfall063.chainmining.ChainMiningStateManager;
import github.starfall063.chainmining.ChainShapeMode;
import github.starfall063.chainmining.network.ChainMiningNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.UUID;

public class ChainMiningInputHandler {
    private UUID syncedPlayerId;
    private Integer syncedDimension;
    private boolean lastSentEnabled;
    private ChainShapeMode lastSentShape = ChainShapeMode.SHAPELESS;
    private BlockMatchMode lastSentMatchMode = BlockMatchMode.META_ONLY;
    private int lastSentNeighborRange = 1;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || minecraft.world == null) {
            this.syncedPlayerId = null;
            this.syncedDimension = null;
            this.lastSentEnabled = false;
            this.lastSentShape = ChainShapeMode.SHAPELESS;
            this.lastSentMatchMode = BlockMatchMode.META_ONLY;
            this.lastSentNeighborRange = 1;
            ChainMiningStateManager.setClientEnabled(false);
            ChainMiningStateManager.clearSyncedConfig();
            return;
        }

        KeyBinding keyBinding = ChainMiningKeyBindings.TOGGLE_CHAIN_MINING;
        ItemStack held = player.getHeldItemMainhand();
        boolean enabled = keyBinding.isKeyDown() && ChainMiningHooks.canUseAnyChainAction(player, held);
        ChainMiningStateManager.setClientEnabled(enabled);

        syncStateIfNeeded(player.getUniqueID(), minecraft.world.provider.getDimension(),
                ChainMiningStateManager.getClientShape(), ChainMiningStateManager.getClientMatchMode(),
                ChainMiningStateManager.getEffectiveNeighborRange(), enabled);
    }

    @SubscribeEvent
    public void onMouseScroll(MouseEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || minecraft.world == null || minecraft.currentScreen != null || !ChainMiningStateManager.isClientEnabled()) {
            return;
        }

        ItemStack held = player.getHeldItemMainhand();
        if (!ChainMiningHooks.canUseAnyChainAction(player, held)) {
            return;
        }

        if (!minecraft.gameSettings.keyBindSneak.isKeyDown() || event.getDwheel() == 0) {
            return;
        }

        boolean ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);

        if (ctrlDown) {
            cycleMatchMode(event.getDwheel());
        } else if (altDown) {
            adjustNeighborRange(event.getDwheel());
        } else {
            cycleShape(event.getDwheel());
        }

        ChainMiningClientSettings.save();
        syncStateIfNeeded(player.getUniqueID(), minecraft.world.provider.getDimension(),
                ChainMiningStateManager.getClientShape(), ChainMiningStateManager.getClientMatchMode(),
                ChainMiningStateManager.getEffectiveNeighborRange(), true);
        event.setCanceled(true);
    }

    private void cycleMatchMode(int wheel) {
        BlockMatchMode current = ChainMiningStateManager.getClientMatchMode();
        BlockMatchMode next = wheel > 0 ? current.previous() : current.next();
        ChainMiningStateManager.setClientMatchMode(next);
        Minecraft minecraft = Minecraft.getMinecraft();
        showStatus(minecraft.player, ChainMiningLang.tr("message.chainmining.match_mode", ChainMiningLang.tr(next.getTranslationKey())));
    }

    private void adjustNeighborRange(int wheel) {
        int current = ChainMiningStateManager.getEffectiveNeighborRange();
        int next = wheel > 0 ? current - 1 : current + 1;
        if (next < 1) next = 5;
        if (next > 5) next = 1;
        ChainMiningStateManager.setClientNeighborRange(next);
        Minecraft minecraft = Minecraft.getMinecraft();
        showStatus(minecraft.player, ChainMiningLang.tr("message.chainmining.range", next));
    }

    private void cycleShape(int wheel) {
        ChainShapeMode nextShape = wheel > 0
                ? ChainMiningStateManager.getClientShape().previous()
                : ChainMiningStateManager.getClientShape().next();
        ChainMiningStateManager.setClientShape(nextShape);
        Minecraft minecraft = Minecraft.getMinecraft();
        showStatus(minecraft.player, ChainMiningLang.tr("message.chainmining.shape", ChainMiningLang.tr(nextShape.getTranslationKey())));
    }

    private void syncStateIfNeeded(UUID playerId, int dimension, ChainShapeMode shapeMode, BlockMatchMode matchMode, int neighborRange, boolean enabled) {
        if (!shouldSync(playerId, dimension, shapeMode, matchMode, neighborRange, enabled)) {
            return;
        }
        ChainMiningNetwork.sendStateToServer(enabled, shapeMode, matchMode, neighborRange);
        this.syncedPlayerId = playerId;
        this.syncedDimension = dimension;
        this.lastSentEnabled = enabled;
        this.lastSentShape = shapeMode == null ? ChainShapeMode.SHAPELESS : shapeMode;
        this.lastSentMatchMode = matchMode == null ? BlockMatchMode.META_ONLY : matchMode;
        this.lastSentNeighborRange = neighborRange;
    }

    private boolean shouldSync(UUID playerId, int dimension, ChainShapeMode shapeMode, BlockMatchMode matchMode, int neighborRange, boolean enabled) {
        ChainShapeMode effectiveShape = shapeMode == null ? ChainShapeMode.SHAPELESS : shapeMode;
        BlockMatchMode effectiveMatchMode = matchMode == null ? BlockMatchMode.META_ONLY : matchMode;
        return !java.util.Objects.equals(playerId, this.syncedPlayerId)
                || !java.util.Objects.equals(this.syncedDimension, dimension)
                || this.lastSentEnabled != enabled
                || this.lastSentShape != effectiveShape
                || this.lastSentMatchMode != effectiveMatchMode
                || this.lastSentNeighborRange != neighborRange;
    }

    private static void showStatus(EntityPlayerSP player, String message) {
        player.sendStatusMessage(new TextComponentString(message), true);
    }
}
