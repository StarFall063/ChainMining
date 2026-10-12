package github.starfall063.chainmining.item;

import github.starfall063.chainmining.ChainMining;
import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.util.IHasModel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class ItemMiningModifier extends Item implements IHasModel {
    public static final ItemMiningModifier INSTANCE = new ItemMiningModifier();

    public ItemMiningModifier() {
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, "chain_upgrade_core"));
        this.setTranslationKey(Tags.MOD_ID + ".chain_upgrade_core");
        this.setCreativeTab(CreativeTabs.MISC);
        this.setMaxStackSize(1);
    }

    @Override
    public void registerModels() {
        ChainMining.proxy.registerModel(this, 0, "inventory");
    }
}
