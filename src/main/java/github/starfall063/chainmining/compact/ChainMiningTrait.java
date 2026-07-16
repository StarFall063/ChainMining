package github.starfall063.chainmining.compact;

import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.item.ItemMiningModifier;
import net.minecraft.nbt.NBTTagCompound;
import slimeknights.tconstruct.library.modifiers.ModifierAspect;
import slimeknights.tconstruct.tools.modifiers.ToolModifier;

public class ChainMiningTrait extends ToolModifier {
    public ChainMiningTrait() {
        super(Tags.MOD_ID, 0xE65CEB);
        this.addAspects(ModifierAspect.toolOnly, new ModifierAspect.MultiAspect(this, 1, 1, 1));
        this.addItem(ItemMiningModifier.INSTANCE, 1, 1);
    }

    @Override
    public void applyEffect(NBTTagCompound nbtTagCompound, NBTTagCompound nbtTagCompound1) {}

    @Override
    public String getTooltip(NBTTagCompound modifierTag, boolean detailed) {
        return getLeveledTooltip(modifierTag, detailed);
    }
}
