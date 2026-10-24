package com.oliver.erydonthemelios.client.migration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oliver.erydonthemelios.migration.ThemeliosIdMigration;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class IdAliasSearchVocabularyTest {
    @Test
    void everyPlacedBlockHasCachedImmutableSearchVocabulary() throws IOException {
        try (var paths = Files.list(Path.of("src/main/resources/assets/themelios/blockstates"))) {
            List<String> ids = paths.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5)).toList();
            assertEquals(2_940, ids.size(), "Catalogue coverage changed; review search categories");
            for (String id : ids) {
                List<String> terms = IdAliasSearchVocabulary.termsForPath(id);
                assertFalse(terms.isEmpty(), id);
                assertSame(terms, IdAliasSearchVocabulary.termsForPath(id), id);
            }
        }
    }

    @Test
    void commonShapesAreSearchableWithoutAnIdMigration() {
        assertTerms("aganite_stairs", "steps", "staircase");
        assertTerms("aganite_slab", "half block");
        assertTerms("aganite_post", "beam", "rod");
        assertTerms("aganite_slice_horizontal", "horizontal slice", "eighth");
        assertTerms("aganite_slice_vertical", "vertical slice", "thin");
        assertTerms("aganite_layer_vertical", "cladding", "panel");
        assertTerms("aganite_cylinder_large", "columns", "pillar", "round");
        assertTerms("aganite_cornice_small", "moulding", "molding", "trim");
        assertTerms("aganite_rock_block", "rough stone", "raw stone");
        assertTerms("aganite_guilloche_silver_block", "byzantine", "metal inlay");
        assertTerms("aganite_rosette_bronze_block", "rose", "rosette inlay");
        assertTerms("aganite_cylinder_small", "polished", "honed", "ganosis", "mirror");
        for (String path : List.of("aganite_aged_block", "aganite_rock_stairs", "aganite_ashlar_layer",
                "aganite_hewn_post", "aganite_rusticated_cylinder_large", "aganite_guilloche_bronze_block")) {
            assertFalse(IdAliasSearchVocabulary.termsForPath(path).contains("polished"), path);
        }
    }

    @Test
    void everyPublishedAliasSurvivesAndSharesItsCanonicalIndexEntry() {
        for (var entry : ThemeliosIdMigration.aliases()) {
            List<String> actual = IdAliasSearchVocabulary.termsForPath(entry.canonicalId().getPath());
            assertTrue(actual.containsAll(entry.searchTerms()), entry.oldId().toString());
            assertSame(actual, IdAliasSearchVocabulary.termsForPath(entry.oldId().getPath()));
            assertThrows(UnsupportedOperationException.class, () -> actual.add("mutation"));
        }
    }

    @Test
    void reiCachePreservesAliasTermsAndWordBoundaries() {
        List<String> aliases = IdAliasSearchVocabulary.termsForPath("aganite_guilloche_silver_block");
        String indexed = IdAliasSearchVocabulary.appendTerms("silver guilloche aganite block", aliases, false);
        for (String term : aliases) {
            assertTrue(indexed.contains(term.toLowerCase(Locale.ROOT)), term);
        }
        assertTrue(indexed.contains("byzantine"));
        assertTrue(indexed.contains("metal inlay"));
        assertTrue(indexed.contains("themelios:aganite_block_silverguilloche"));
        assertFalse(indexed.contains("blockbyzantine"), "Separate aliases must not form a false word");
        assertEquals("\u0000verticalslice", IdAliasSearchVocabulary.appendTerms(null,
                List.of("Vertical Slice"), true));
    }

    @Test
    void foreignItemsDoNotAddSearchTextOrAllocateAReplacementString() {
        assertTrue(IdAliasSearchVocabulary.terms(Identifier.of("minecraft", "stone")).isEmpty());
        String original = new String("stone");
        assertSame(original, IdAliasSearchVocabulary.appendTerms(original, List.of(), false));
        assertNull(IdAliasSearchVocabulary.appendTerms(null, List.of(), false));
    }

    @Test
    void everySearchGroupHasMatchingEnglishGermanAndSpanishResources() throws IOException {
        Set<String> expected = Set.copyOf(IdAliasSearchVocabulary.translationKeys());
        assertEquals(23, expected.size());
        for (String locale : List.of("en_us", "de_de", "es_es")) {
            JsonObject entries = languageEntries(locale);
            Set<String> actual = entries.keySet().stream()
                    .filter(key -> key.startsWith("search.themelios.")).collect(Collectors.toSet());
            assertEquals(expected, actual, locale);
            for (String key : expected) {
                for (String term : entries.get(key).getAsString().split("\\|", -1)) {
                    assertFalse(term.isBlank(), locale + " / " + key);
                }
            }
        }
    }

    @Test
    void languageSwitchReplacesTheCacheWhileKeepingEnglishAndPublishedAliases() throws IOException {
        Language original = Language.getInstance();
        try {
            Language german = language(languageEntries("de_de"));
            Language.setInstance(german);
            List<String> germanStairs = IdAliasSearchVocabulary.termsForPath("aganite_stairs");
            assertTrue(germanStairs.containsAll(List.of("stufen", "treppe", "steps")));
            assertTerms("aganite_cylinder_small", "poliert", "geschliffen", "spiegelglanz");
            assertTerms("aganite_cornice_small", "gesims", "zierleiste");
            assertSame(germanStairs, IdAliasSearchVocabulary.termsForPath("aganite_stairs"));
            Language.setInstance(german);
            assertSame(germanStairs, IdAliasSearchVocabulary.termsForPath("aganite_stairs"));

            Language.setInstance(language(languageEntries("es_es")));
            List<String> spanishStairs = IdAliasSearchVocabulary.termsForPath("aganite_stairs");
            assertNotSame(germanStairs, spanishStairs);
            assertTrue(spanishStairs.containsAll(List.of("peldaños", "escalera", "steps")));
            assertFalse(spanishStairs.contains("treppe"), "The retired language cache must not leak");
            assertTerms("aganite_cylinder_small", "pulido", "apomazado", "espejo");
            assertTerms("aganite_cornice_small", "cornisa", "moldura");
            List<String> rosette = IdAliasSearchVocabulary.termsForPath("aganite_rosette_bronze_block");
            assertTrue(rosette.containsAll(List.of("rosa", "incrustación", "rose",
                    "themelios:aganite_block_bronzerose")));
            assertSame(rosette, IdAliasSearchVocabulary.termsForPath("aganite_block_bronzerose"));

            Language.setInstance(german);
            List<String> restored = IdAliasSearchVocabulary.termsForPath("aganite_stairs");
            assertEquals(germanStairs, restored);
            assertNotSame(germanStairs, restored, "Retired cache maps must be released");
        } finally {
            Language.setInstance(original);
        }
    }

    @Test
    void unavailableTranslationsKeepEnglishWithoutIndexingMissingKeys() {
        Language original = Language.getInstance();
        try {
            Language.setInstance(language(new JsonObject()));
            List<String> terms = IdAliasSearchVocabulary.termsForPath("aganite_post");
            assertTrue(terms.containsAll(List.of("beam", "rod", "polished")));
            assertTrue(terms.stream().noneMatch(term -> term.startsWith("search.themelios.")));
            assertTrue(IdAliasSearchVocabulary.termsForPath("unrecognised_shape").isEmpty());
        } finally {
            Language.setInstance(original);
        }
    }

    private static JsonObject languageEntries(String locale) throws IOException {
        try (var reader = Files.newBufferedReader(Path.of(
                "src/main/resources/assets/themelios/lang/" + locale + ".json"))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Language language(JsonObject entries) {
        return new Language() {
            @Override public String get(String key, String fallback) {
                return entries.has(key) ? entries.get(key).getAsString() : fallback;
            }
            @Override public boolean hasTranslation(String key) { return entries.has(key); }
            @Override public boolean isRightToLeft() { return false; }
            @Override public OrderedText reorder(StringVisitable text) { return OrderedText.EMPTY; }
        };
    }

    private static void assertTerms(String path, String... expected) {
        assertTrue(IdAliasSearchVocabulary.termsForPath(path).containsAll(List.of(expected)), path);
    }
}
