package com.oliver.erydonthemelios.client.finish;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErydonStoneFinishBridgeTest {
    @Test
    void optionalBindingUsesTheCapturedRendererSnapshotAndItsActualClassifier() throws Exception {
        Preferences.active = new Settings(Map.of("glacium_block", Level.HONED,
                "latmion_herringbone_bronze_block", Level.POLISHED, "calacattum_portorium_weave_bronze_block", Level.MIRROR));
        var result = ErydonStoneFinishBridge.read(Preferences.class, Materials.class, List.of(
                "glacium_block", "latmion_herringbone_bronze_block", "calacattum_portorium_weave_bronze_block",
                "glacium_aged_block", "aganite_ashlar_block", "aganite_rock_block"));
        assertEquals(Map.of("glacium_block", "honed", "latmion_herringbone_bronze_block", "polished",
                "calacattum_portorium_weave_bronze_block", "mirror"), result);
        Preferences.active = new Settings(Map.of("glacium_block", Level.MIRROR));
        assertEquals("honed", result.get("glacium_block"), "A saved preference is inactive until restart, like rendering");
        assertThrows(UnsupportedOperationException.class, () -> result.put("glacium_block", "mirror"));
    }

    @Test
    void absentOrOlderOptionalApiCanBeRejectedWithoutLinkingErydonTypes() {
        ClassLoader absent = new ClassLoader(null) { };
        assertThrows(ClassNotFoundException.class, () -> ErydonStoneFinishBridge.read(absent, List.of("glacium_block")));
        assertThrows(NoSuchMethodException.class, () -> ErydonStoneFinishBridge.read(Object.class, Materials.class, List.of()));
    }

    public enum Level { HONED, POLISHED, MIRROR }
    public record Settings(Map<String, Level> values) { }
    public static final class Preferences {
        private static Settings active;
        public static Settings activeSettings() { return active; }
    }
    public static final class Materials {
        public static boolean includes(String namespace, String path) {
            return namespace.equals("themelios") && Preferences.active.values.containsKey(path);
        }
        public static Level level(Settings settings, String path) { return settings.values.get(path); }
    }
}
