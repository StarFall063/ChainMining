package github.starfall063.chainmining.client;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public final class ChainMiningKeyBindings {
    public static final KeyBinding CHAIN_MINING_KEY = new KeyBinding(
            "key.chainmining.enable",
            Keyboard.KEY_GRAVE,
            "key.categories.chainmining"
    );

    private ChainMiningKeyBindings() {
    }
}
