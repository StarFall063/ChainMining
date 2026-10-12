package github.starfall063.chainmining.item;

import github.starfall063.chainmining.ChainMining;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.util.IHasModel;
import appeng.api.features.INetworkEncodable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import baubles.api.BaublesApi;
import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.List;

@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public final class ItemChainStorageTerminal extends Item implements IHasModel, INetworkEncodable, IBauble {
    public static final ItemChainStorageTerminal INSTANCE = new ItemChainStorageTerminal();
    private static final String ENABLED = "ChainStorageEnabled";
    private static final String ENCRYPTION_KEY = "encryptionKey";

    private ItemChainStorageTerminal() {
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, "chain_storage_terminal"));
        this.setTranslationKey(Tags.MOD_ID + ".chain_storage_terminal");
        this.setCreativeTab(CreativeTabs.MISC);
        this.setMaxStackSize(1);
    }

    public static boolean isEnabled(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTagCompound() && stack.getTagCompound().getBoolean(ENABLED);
    }

    public static void setEnabled(ItemStack stack, boolean enabled) {
        if (stack.isEmpty()) return;
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setBoolean(ENABLED, enabled);
        stack.setTagCompound(tag);
    }

    public static void setEnabled(EntityPlayer player, ItemStack target, boolean enabled) {
        IItemHandler baubles = null;
        int targetBaubleSlot = -1;
        if (isBaublesLoaded()) {
            baubles = BaublesApi.getBaublesHandler(player);
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    if (baubles.getStackInSlot(i) == target) {
                        targetBaubleSlot = i;
                        break;
                    }
                }
            }
        }
        if (enabled) {
            for (ItemStack stack : player.inventory.mainInventory) {
                if (stack != target && stack.getItem() == INSTANCE) setEnabled(stack, false);
            }
            if (baubles != null) {
                for (int i = 0; i < baubles.getSlots(); i++) {
                    ItemStack stack = baubles.getStackInSlot(i);
                    if (!stack.isEmpty() && i != targetBaubleSlot && stack.getItem() == INSTANCE) {
                        setHandlerEnabled(baubles, i, false);
                    }
                }
            }
        }
        if (targetBaubleSlot >= 0 && baubles != null) {
            setHandlerEnabled(baubles, targetBaubleSlot, enabled);
        } else {
            setEnabled(target, enabled);
        }
    }

    public static boolean setHandlerEnabled(IItemHandler handler, int slot, boolean enabled) {
        if (!(handler instanceof IItemHandlerModifiable) || slot < 0 || slot >= handler.getSlots()) return false;
        ItemStack stack = handler.getStackInSlot(slot);
        if (stack.isEmpty() || stack.getItem() != INSTANCE) return false;
        ItemStack updated = stack.copy();
        setEnabled(updated, enabled);
        ((IItemHandlerModifiable) handler).setStackInSlot(slot, updated);
        return true;
    }

    public static boolean isBaublesLoaded() {
        return Loader.isModLoaded("baubles");
    }

    public static boolean isBound(ItemStack stack) {
        return !getSecurityKey(stack).isEmpty();
    }

    public static String getSecurityKey(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return "";
        return stack.getTagCompound().getString(ENCRYPTION_KEY);
    }

    public static void setSecurityKey(ItemStack stack, String key) {
        if (stack.isEmpty()) return;
        if (key == null || key.isEmpty()) {
            clearBinding(stack);
            return;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setString(ENCRYPTION_KEY, key);
        stack.setTagCompound(tag);
    }

    public static void clearBinding(ItemStack stack) {
        if (!stack.isEmpty() && stack.hasTagCompound()) {
            stack.getTagCompound().removeTag(ENCRYPTION_KEY);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            if (player.isSneaking()) clearBinding(stack);
            else setEnabled(player, stack, !isEnabled(stack));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.translateToLocal("item.chainmining.chain_storage_terminal.state")
                + I18n.translateToLocal(isEnabled(stack)
                ? "item.chainmining.chain_storage_terminal.enabled"
                : "item.chainmining.chain_storage_terminal.disabled"));
        tooltip.add(I18n.translateToLocal("item.chainmining.chain_storage_terminal.bind_state") + (isBound(stack)
                ? I18n.translateToLocal("item.chainmining.chain_storage_terminal.bound")
                : I18n.translateToLocal("item.chainmining.chain_storage_terminal.unbound")));
        tooltip.add(I18n.translateToLocal("item.chainmining.chain_storage_terminal.tooltip1"));
        tooltip.add(I18n.translateToLocal("item.chainmining.chain_storage_terminal.tooltip2"));
        tooltip.add(I18n.translateToLocal("item.chainmining.chain_storage_terminal.tooltip3"));
    }

    @Override
    public String getEncryptionKey(ItemStack stack) {
        return getSecurityKey(stack);
    }

    @Override
    public void setEncryptionKey(ItemStack stack, String encryptionKey, String name) {
        setSecurityKey(stack, encryptionKey);
    }

    @Override
    public void registerModels() {
        ChainMining.proxy.registerModel(this, 0, "inventory");
    }

    @Override
    @Optional.Method(modid = "baubles")
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET;
    }

}
