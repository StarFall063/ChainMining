package github.starfall063.chainmining;

public enum BlockMatchMode {
    META_ONLY("meta", "match.chainmining.meta_only"),
    NBT_ONLY("nbt", "match.chainmining.nbt_only"),
    NBT_META("nbt_meta", "match.chainmining.nbt_meta"),
    REGISTRY_ONLY("registry", "match.chainmining.registry_only");

    private final String serializedName;
    private final String translationKey;

    BlockMatchMode(String serializedName, String translationKey) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public boolean shouldMatchMeta() {
        return this == META_ONLY || this == NBT_META;
    }

    public boolean shouldMatchNbt() {
        return this == NBT_ONLY || this == NBT_META;
    }

    public BlockMatchMode next() {
        BlockMatchMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public BlockMatchMode previous() {
        BlockMatchMode[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }

    public static BlockMatchMode fromName(String name) {
        if (name == null || name.isEmpty()) {
            return META_ONLY;
        }
        for (BlockMatchMode mode : values()) {
            if (mode.serializedName.equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return META_ONLY;
    }
}
