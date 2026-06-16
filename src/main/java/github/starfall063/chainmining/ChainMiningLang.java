package github.starfall063.chainmining;

import javax.annotation.Nullable;

@SuppressWarnings("deprecation")
public final class ChainMiningLang {
    private ChainMiningLang() {
    }

    public static String tr(String key) {
        String result = net.minecraft.util.text.translation.I18n.translateToLocal(key);
        return result.equals(key) ? key : result;
    }

    public static String tr(String key, @Nullable Object... args) {
        if (args == null || args.length == 0) {
            return tr(key);
        }
        return net.minecraft.util.text.translation.I18n.translateToLocalFormatted(key, args);
    }
}
