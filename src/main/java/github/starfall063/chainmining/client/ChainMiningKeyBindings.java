package github.starfall063.chainmining.client;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public final class ChainMiningKeyBindings {
    public static final String CATEGORY = "key.categories.chainmining";
    public static final KeyBinding TOGGLE_CHAIN_MINING = new KeyBinding(
            "key.chainmining.toggle_chain_mining",
            Keyboard.KEY_F6,
            CATEGORY
    );

    private ChainMiningKeyBindings() {
    }
}
