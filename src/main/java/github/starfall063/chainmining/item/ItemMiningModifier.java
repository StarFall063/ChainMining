package github.starfall063.chainmining.item;

import github.starfall063.chainmining.ChainMining;
import github.starfall063.chainmining.ChainMiningConfig;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.util.IHasModel;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class ItemMiningModifier extends Item implements IHasModel {
    public static final ItemMiningModifier INSTANCE = new ItemMiningModifier();

    public ItemMiningModifier() {
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, "chain_upgrade_core"));
        this.setTranslationKey(Tags.MOD_ID + ".chain_upgrade_core");
        this.setMaxStackSize(1);
    }

    @SubscribeEvent
    public static void registerItem(RegistryEvent.Register<Item> event) {
        if (ChainMiningConfig.SERVER.chainMiningRequireEnchantment) {
            event.getRegistry().register(ItemMiningModifier.INSTANCE);
        }
    }

    @Override
    public void registerModels() {
        ChainMining.proxy.registerModel(this, 0, "inventory");
    }
}
