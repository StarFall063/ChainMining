package github.starfall063.chainmining.enchantment;

import github.starfall063.chainmining.Tags;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;

public class ChainMiningEnchantment extends Enchantment {
    protected ChainMiningEnchantment() {
        super(Rarity.VERY_RARE, EnumEnchantmentType.DIGGER, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
        this.setRegistryName(new ResourceLocation(Tags.MOD_ID, "chainmining"));
        this.setName(Tags.MOD_ID);
    }

    @Override
    public int getMinEnchantability(int level) {
        return 30;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isTreasureEnchantment() {
        return false;
    }
}
