package github.starfall063.chainmining.config;

import github.starfall063.chainmining.ChainMiningConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChainMiningGuiFactory implements IModGuiFactory {
    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiConfig(parentScreen, getConfigElements(),
                "chainmining", "chainmining.cfg", false, false, "Chain Mining Config");
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return Collections.emptySet();
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> elements = new ArrayList<>();
        if (ChainMiningConfig.CONFIG != null) {
            for (String category : ChainMiningConfig.CONFIG.getCategoryNames()) {
                elements.add(new ConfigElement(ChainMiningConfig.CONFIG.getCategory(category)));
            }
        }
        return elements;
    }
}
