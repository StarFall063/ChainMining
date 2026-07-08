package github.starfall063.chainmining.util;

public enum ChainShapeMode {
    SHAPELESS("shapeless", "shape.chainmining.shapeless"),
    PLANE("plane", "shape.chainmining.plane"),
    TUNNEL("tunnel", "shape.chainmining.tunnel");

    private final String serializedName;
    private final String translationKey;

    ChainShapeMode(String serializedName, String translationKey) {
        this.serializedName = serializedName;
        this.translationKey = translationKey;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public ChainShapeMode next() {
        ChainShapeMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public ChainShapeMode previous() {
        ChainShapeMode[] values = values();
        return values[(this.ordinal() - 1 + values.length) % values.length];
    }

    public static ChainShapeMode fromName(String name) {
        if (name == null) return SHAPELESS;
        for (ChainShapeMode mode : values()) {
            if (mode.serializedName.equalsIgnoreCase(name)) return mode;
        }
        return SHAPELESS;
    }
}
