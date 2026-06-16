package github.starfall063.chainmining;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChainShapeModeTest {
    @Test
    void fromNameAcceptsSerializedAndEnumNamesCaseInsensitively() {
        assertEquals(ChainShapeMode.SHAPELESS, ChainShapeMode.fromName(null));
        assertEquals(ChainShapeMode.SHAPELESS, ChainShapeMode.fromName("   "));
        assertEquals(ChainShapeMode.PLANE, ChainShapeMode.fromName("plane"));
        assertEquals(ChainShapeMode.TUNNEL, ChainShapeMode.fromName("TUNNEL"));
        assertEquals(ChainShapeMode.SHAPELESS, ChainShapeMode.fromName("missing"));
    }

    @Test
    void nextAndPreviousWrapAroundAvailableModes() {
        assertEquals(ChainShapeMode.PLANE, ChainShapeMode.SHAPELESS.next());
        assertEquals(ChainShapeMode.TUNNEL, ChainShapeMode.PLANE.next());
        assertEquals(ChainShapeMode.SHAPELESS, ChainShapeMode.TUNNEL.next());

        assertEquals(ChainShapeMode.TUNNEL, ChainShapeMode.SHAPELESS.previous());
        assertEquals(ChainShapeMode.SHAPELESS, ChainShapeMode.PLANE.previous());
        assertEquals(ChainShapeMode.PLANE, ChainShapeMode.TUNNEL.previous());
    }
}
