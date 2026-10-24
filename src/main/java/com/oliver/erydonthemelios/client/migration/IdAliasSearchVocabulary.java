package com.oliver.erydonthemelios.client.migration;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.migration.ThemeliosIdMigration;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Shared, lazily indexed search vocabulary for optional item browsers and Axiom. */
public final class IdAliasSearchVocabulary {
    private static final List<String> NONSTANDARD_FINISHES = List.of(
            "_aged_", "_ashlar_", "_hewn_", "_rusticated_", "_rock_", "_herringbone_", "_weave_",
            "_trim_", "_guilloche_", "_quatrefoil_", "_rosette_");

    private IdAliasSearchVocabulary() {
    }

    public static List<String> terms(ItemStack stack) {
        return terms(Registries.ITEM.getId(stack.getItem()));
    }

    public static List<String> terms(Identifier canonicalId) {
        if (canonicalId == null || !ErydonThemelios.MOD_ID.equals(canonicalId.getNamespace())) {
            return List.of();
        }
        return termsForPath(canonicalId.getPath());
    }

    static List<String> termsForPath(String path) {
        String canonicalPath = ThemeliosIdMigration.canonicalPath(path);
        TermsCache cache = TermsCache.current();
        return cache.byPath.computeIfAbsent(canonicalPath, cache);
    }

    private static List<String> createTerms(String path, Map<AliasGroup, List<String>> groups) {
        LinkedHashSet<String> terms = new LinkedHashSet<>(
                ThemeliosIdMigration.searchTermsForCanonicalPath(path));
        AliasGroup shape = shapeGroup(path);
        if (shape == null) return List.copyOf(terms);
        terms.addAll(groups.get(shape));
        if (hasStandardFinish(path)) {
            terms.addAll(groups.get(AliasGroup.STANDARD_SURFACES));
        }
        if (path.contains("_aged_")) terms.addAll(groups.get(AliasGroup.AGED));
        if (path.contains("_rock_")) terms.addAll(groups.get(AliasGroup.ROCK));
        if (path.contains("_ashlar_")) terms.addAll(groups.get(AliasGroup.ASHLAR));
        if (path.contains("_rusticated_")) terms.addAll(groups.get(AliasGroup.RUSTICATED));
        if (path.contains("_hewn_")) terms.addAll(groups.get(AliasGroup.HEWN));
        if (path.contains("_weave_")) terms.addAll(groups.get(AliasGroup.WEAVE));
        if (path.contains("_herringbone_")) terms.addAll(groups.get(AliasGroup.PATTERNED));
        if (path.contains("_grout_")) terms.addAll(groups.get(AliasGroup.GROUTED));
        if (path.contains("_bronze_") || path.contains("_silver_")) {
            terms.addAll(groups.get(AliasGroup.METAL_INLAY));
        }
        if (path.contains("_guilloche_")) terms.addAll(groups.get(AliasGroup.GUILLOCHE));
        if (path.contains("_rosette_")) terms.addAll(groups.get(AliasGroup.ROSETTE));
        return List.copyOf(terms);
    }

    private static boolean hasStandardFinish(String path) {
        for (String fragment : NONSTANDARD_FINISHES) {
            if (path.contains(fragment)) return false;
        }
        return true;
    }

    private static AliasGroup shapeGroup(String path) {
        if (path.endsWith("_slab")) return AliasGroup.SLABS;
        if (path.endsWith("_stairs")) return AliasGroup.STAIRS;
        if (path.endsWith("_wall")) return AliasGroup.WALLS;
        if (path.endsWith("_post")) return AliasGroup.POSTS;
        if (path.endsWith("_slice_horizontal")) {
            return AliasGroup.HORIZONTAL_SLICES;
        }
        if (path.endsWith("_slice_vertical")) {
            return AliasGroup.VERTICAL_SLICES;
        }
        if (path.endsWith("_layer_vertical")) {
            return AliasGroup.VERTICAL_LAYERS;
        }
        if (path.endsWith("_layer")) return AliasGroup.LAYERS;
        if (path.endsWith("_cylinder_small") || path.endsWith("_cylinder_large")) {
            return AliasGroup.CYLINDERS;
        }
        if (path.endsWith("_cornice_small") || path.endsWith("_cornice_large")) {
            return AliasGroup.CORNICES;
        }
        if (path.endsWith("_block")) return AliasGroup.BLOCKS;
        return null;
    }

    static List<String> translationKeys() {
        return java.util.Arrays.stream(AliasGroup.values()).map(group -> group.key).toList();
    }

    private enum AliasGroup {
        SLABS("slabs", "slabs", "half block"),
        STAIRS("stairs", "steps", "staircase"),
        WALLS("walls", "walls"),
        POSTS("posts", "posts", "beam", "rod"),
        HORIZONTAL_SLICES("horizontal_slices", "slices", "horizontal slice", "eighth", "thin"),
        VERTICAL_SLICES("vertical_slices", "slices", "vertical slice", "eighth", "thin"),
        LAYERS("layers", "layers", "cladding", "panel", "thin"),
        VERTICAL_LAYERS("vertical_layers", "layers", "vertical layer", "cladding", "panel", "thin"),
        CYLINDERS("cylinders", "cylinders", "column", "columns", "pillar", "round"),
        CORNICES("cornices", "cornices", "moulding", "molding", "trim"),
        BLOCKS("blocks", "building block"),
        STANDARD_SURFACES("standard_surfaces", "polished", "honed", "ganosis", "mirror"),
        AGED("aged", "weathered"),
        ROCK("rock", "rough stone", "raw stone"),
        ASHLAR("ashlar", "masonry", "cut stone"),
        RUSTICATED("rusticated", "rustic stone"),
        HEWN("hewn", "rough cut stone"),
        WEAVE("weave", "woven", "patterned"),
        PATTERNED("patterned", "patterned"),
        GROUTED("grouted", "grouted"),
        METAL_INLAY("metal_inlay", "inlay", "metal inlay", "decorative"),
        GUILLOCHE("guilloche", "byzantine", "guilloche motif"),
        ROSETTE("rosette", "rose", "rosette inlay");

        private final String key;
        private final List<String> english;

        AliasGroup(String name, String... english) {
            this.key = "search.themelios." + name;
            this.english = List.of(english);
        }

        private List<String> resolve(Language language) {
            LinkedHashSet<String> terms = new LinkedHashSet<>(english);
            for (String term : language.get(key, "").split("\\|")) {
                if (!term.isBlank()) terms.add(term.strip());
            }
            return List.copyOf(terms);
        }
    }

    // REI indexes on worker threads. Resolve each synonym group once per language,
    // then memoize immutable item lists. Replacing Language retires the old index;
    // no reload listener, normal tooltip, tick or render work is introduced.
    private static final class TermsCache implements Function<String, List<String>> {
        private static volatile TermsCache active;
        private final Language language;
        private final Map<String, List<String>> byPath = new ConcurrentHashMap<>();
        private final Map<AliasGroup, List<String>> groups = new EnumMap<>(AliasGroup.class);

        private TermsCache(Language language) {
            this.language = language;
            for (AliasGroup group : AliasGroup.values()) groups.put(group, group.resolve(language));
        }

        private static TermsCache current() {
            Language language = Language.getInstance();
            TermsCache cache = active;
            return cache != null && cache.language == language ? cache : replace();
        }

        private static synchronized TermsCache replace() {
            Language language = Language.getInstance();
            if (active == null || active.language != language) active = new TermsCache(language);
            return active;
        }

        @Override
        public List<String> apply(String path) {
            return createTerms(path, groups);
        }
    }

    public static String appendSearchTerms(String existing, ItemStack stack, boolean removeWhitespace) {
        return appendTerms(existing, terms(stack), removeWhitespace);
    }

    public static String appendTerms(String existing, List<String> terms, boolean removeWhitespace) {
        if (terms.isEmpty()) {
            return existing;
        }
        StringBuilder builder = new StringBuilder(existing == null ? "" : existing);
        for (String term : terms) {
            builder.append('\u0000').append(normalize(term, removeWhitespace));
        }
        return builder.toString();
    }

    static String normalize(String value, boolean removeWhitespace) {
        String lowered = value.toLowerCase(Locale.ROOT);
        if (!removeWhitespace) {
            return lowered;
        }
        StringBuilder builder = new StringBuilder(lowered.length());
        for (int index = 0; index < lowered.length(); index++) {
            char character = lowered.charAt(index);
            if (!Character.isWhitespace(character)) {
                builder.append(character);
            }
        }
        return builder.toString();
    }
}
