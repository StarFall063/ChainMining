package github.starfall063.chainmining.util;

import java.util.regex.Pattern;
import java.util.Locale;

public final class SneakGrowthFilter {
    private SneakGrowthFilter() {
    }

    public static boolean isAllowed(String registryName, int meta, String[] whitelist, String[] blacklist) {
        String id = registryName == null ? "" : registryName.trim().toLowerCase(Locale.ROOT);
        String fullId = id + ":" + meta;
        if (matchesAny(blacklist, id, fullId)) return false;
        return whitelist == null || whitelist.length == 0 || matchesAny(whitelist, id, fullId);
    }

    private static boolean matchesAny(String[] entries, String registryName, String fullId) {
        if (entries == null) return false;
        for (String entry : entries) {
            if (entry == null) continue;
            String pattern = entry.trim().toLowerCase(Locale.ROOT);
            if (pattern.isEmpty()) continue;
            if (matches(pattern, registryName) || matches(pattern, fullId)) return true;
        }
        return false;
    }

    private static boolean matches(String pattern, String text) {
        String[] parts = pattern.split("\\*", -1);
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) regex.append(".*");
            regex.append(Pattern.quote(parts[i]));
        }
        return text.matches(regex.toString());
    }
}
