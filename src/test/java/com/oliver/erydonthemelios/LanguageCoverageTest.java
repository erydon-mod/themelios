package com.oliver.erydonthemelios;

import com.google.gson.JsonParser;
import com.oliver.erydonthemelios.migration.ThemeliosIdMigration;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Checks the actual registration lists, including generated Rock/cylinder/cornice IDs. */
final class LanguageCoverageTest {
    @Test
    void everyRegisteredCanonicalBlockHasANameInEveryLanguage() throws Exception {
        Set<String> expected = Stream.of(ModBlockIds.FULL_BLOCKS, ModBlockIds.SLABS,
                        ModBlockIds.STAIRS, ModBlockIds.WALLS, ModBlockIds.POSTS,
                        ModBlockIds.HORIZONTAL_SLICES, ModBlockIds.VERTICAL_SLICES,
                        ModBlockIds.LAYERS, ModBlockIds.VERTICAL_LAYERS,
                        ModBlockIds.CYLINDER_SMALLS, ModBlockIds.CYLINDER_LARGES,
                        ModBlockIds.CORNICE_SMALLS, ModBlockIds.CORNICE_LARGES)
                .flatMap(List::stream).map(ThemeliosIdMigration::canonicalPath)
                .map(path -> "block.themelios." + path).collect(Collectors.toSet());
        assertEquals(2940, expected.size());
        Set<String> englishKeys = null;
        for (String language : List.of("en_us", "de_de", "es_es")) {
            Path path = Path.of("src/main/resources/assets/themelios/lang", language + ".json");
            Set<String> keys = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet();
            assertEquals(expected, keys.stream().filter(key -> key.startsWith("block.themelios."))
                    .collect(Collectors.toSet()), language + " must name every registered block exactly once");
            if (englishKeys == null) englishKeys = Set.copyOf(keys);
            assertEquals(englishKeys, keys, language + " must retain all UI and search vocabulary keys");
        }
    }

    @Test
    void everyExportedItemTagHasALabelInEveryLanguage() throws Exception {
        String properties = Files.readString(Path.of("gradle.properties"), StandardCharsets.UTF_8);
        String folder = properties.contains("minecraft_version=1.21.1") ? "item" : "items";
        Set<String> expected = new HashSet<>();
        for (String namespace : List.of("themelios", "material", "color", "style")) {
            Path base = Path.of("src/main/resources/data", namespace, "tags", folder);
            try (var paths = Files.walk(base)) {
                paths.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                    String name = base.relativize(path).toString().replace('\\', '/');
                    name = name.substring(0, name.length() - ".json".length()).replace('/', '.');
                    expected.add("tag.item." + namespace + "." + name);
                });
            }
        }
        assertEquals(131, expected.size());
        for (String language : List.of("en_us", "de_de", "es_es")) {
            Path path = Path.of("src/main/resources/assets/themelios/lang", language + ".json");
            Set<String> keys = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                    .getAsJsonObject().keySet();
            assertEquals(expected, keys.stream().filter(key -> key.startsWith("tag.item."))
                    .collect(Collectors.toSet()), language + " must label each exported item tag");
        }
    }
}
