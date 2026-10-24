package com.oliver.erydonthemelios.compat.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThemeliosJeiInfoTest {
    @Test
    void everyPageHasRegisteredItemsAndEveryShapeHasOneShapePage() throws Exception {
        List<String> paths;
        try (var files = Files.list(Path.of("src/main/resources/assets/themelios/blockstates"))) {
            paths = files.map(path -> path.getFileName().toString().replace(".json", "")).toList();
        }
        assertEquals(2940, paths.size());
        for (ThemeliosJeiInfo page : ThemeliosJeiInfo.values()) {
            assertTrue(paths.stream().anyMatch(page::matches), page.translationKey());
        }
        for (String path : paths) {
            long shapePages = Arrays.stream(ThemeliosJeiInfo.values())
                    .filter(page -> page != ThemeliosJeiInfo.WEAVES && page.matches(path)).count();
            assertEquals(path.endsWith("_block") ? 0 : 1, shapePages, path);
            assertEquals(path.contains("_weave_"), ThemeliosJeiInfo.WEAVES.matches(path), path);
        }
    }

    @Test
    void allInformationPagesHaveExplicitTranslations() throws Exception {
        Set<String> expected = Arrays.stream(ThemeliosJeiInfo.values())
                .map(ThemeliosJeiInfo::translationKey).collect(Collectors.toSet());
        assertEquals(8, expected.size());
        for (String locale : List.of("en_us", "de_de", "es_es")) {
            JsonObject language = JsonParser.parseString(Files.readString(
                    Path.of("src/main/resources/assets/themelios/lang", locale + ".json"))).getAsJsonObject();
            assertEquals(expected, language.keySet().stream()
                    .filter(key -> key.startsWith("jei_info.themelios.")).collect(Collectors.toSet()), locale);
            for (String key : expected) {
                assertTrue(!language.get(key).getAsString().isBlank(), locale + ": " + key);
            }
        }
    }
}
