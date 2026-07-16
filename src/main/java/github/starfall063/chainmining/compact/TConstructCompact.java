package github.starfall063.chainmining.compact;

import github.starfall063.chainmining.Tags;
import github.starfall063.chainmining.item.ItemMiningModifier;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.mantle.client.book.repository.FileRepository;
import slimeknights.tconstruct.library.TinkerRegistry;
import slimeknights.tconstruct.library.book.TinkerBook;
import slimeknights.tconstruct.library.utils.TinkerUtil;

public final class TConstructCompact {
    public static void register() {
        new ChainMiningTrait();
        ItemMiningModifier.INSTANCE.setCreativeTab(TinkerRegistry.tabGeneral);
    }

    public static boolean hasChainMiningModifier(ItemStack tool) {
        if (tool.isEmpty() || !tool.hasTagCompound()) return false;
        return TinkerUtil.hasModifier(tool.getTagCompound(), Tags.MOD_ID);
    }

    @SideOnly(Side.CLIENT)
    public static void registerBookPage() {
        TinkerBook.INSTANCE.addRepository(new FileRepository(Tags.MOD_ID + ":book"));
    }
}
