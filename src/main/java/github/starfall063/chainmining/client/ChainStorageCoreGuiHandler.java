package github.starfall063.chainmining.client;

import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.item.ItemChainStorageTerminal;
import github.starfall063.chainmining.network.ChainMiningNetwork;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class ChainStorageCoreGuiHandler {
    private ChainStorageCoreGuiHandler() {
    }

    @SubscribeEvent
    public static void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer) || Mouse.getEventButton() != 1 || !Mouse.getEventButtonState()) return;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) return;
        GuiContainer gui = (GuiContainer) event.getGui();
        Slot slot = gui.getSlotUnderMouse();
        if (slot == null) return;
        ItemStack stack = slot.getStack();
        if (stack.isEmpty() || stack.getItem() != ItemChainStorageTerminal.INSTANCE) return;
        int slotNumber = slot.slotNumber;
        if (gui instanceof GuiContainerCreative) {
            Minecraft mc = Minecraft.getMinecraft();
            if (slot.inventory != mc.player.inventory) {
                // Creative catalog slots are client-side display stacks. There is no
                // server inventory slot to toggle until the item is taken from the tab.
                ItemStack updated = stack.copy();
                ItemChainStorageTerminal.setEnabled(updated, !ItemChainStorageTerminal.isEnabled(updated));
                slot.putStack(updated);
                event.setCanceled(true);
                return;
            }
            if (slotNumber < 0 || slotNumber >= mc.player.inventoryContainer.inventorySlots.size()) return;
        }
        ChainMiningNetwork.CHANNEL.sendToServer(new ChainMiningNetwork.ToggleStorageCoreMessage(
                gui instanceof GuiContainerCreative ? Minecraft.getMinecraft().player.inventoryContainer.windowId : gui.inventorySlots.windowId,
                slotNumber));
        event.setCanceled(true);
    }
}
