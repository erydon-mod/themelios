package com.oliver.erydonthemelios;

import net.minecraft.block.Block;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class ModBlockOrdering {
    private static final List<String> MATERIALS = List.of(
            "aganite", "aterzon", "borealis", "brectite", "calacattum", "chalstrom", "chrysonyx",
            "etruscus", "gelastrum", "glacium", "hesperion", "imperium", "kylorion", "kelastrion",
            "latmion", "laurentium", "mielonyx", "nerium", "noxoplis", "porphyros", "psamatheon",
            "portorium", "rosinium", "sanguenite", "selenephos", "solistra", "striatus"
    );

    private static final Map<String, Integer> SHAPE_ORDER = Map.ofEntries(
            Map.entry("block", 0),
            Map.entry("slab", 10),
            Map.entry("stairs", 20),
            Map.entry("wall", 30),
            Map.entry("layer", 40),
            Map.entry("layer_multiface", 41),
            Map.entry("layer_vertical", 42),
            Map.entry("vertical_slice", 50),
            Map.entry("horizontal_slice", 51),
            Map.entry("post", 52),
            Map.entry("cylinder_small", 53),
            Map.entry("cylinder_large", 54),
            Map.entry("cornice_small", 55),
            Map.entry("cornice_large", 56)
    );

    private static final Map<String, Integer> DECORATION_ORDER = Map.ofEntries(
            Map.entry("bronzetrim", 0),
            Map.entry("silvertrim", 1),
            Map.entry("bronzeguilloche", 2),
            Map.entry("silverguilloche", 3),
            Map.entry("bronzequatrefoil", 4),
            Map.entry("silverquatrefoil", 5),
            Map.entry("bronzerose", 6),
            Map.entry("silverrose", 7)
    );

    private ModBlockOrdering() {
    }

    static List<Block> orderedBlocks(Map<String, Block> blocks) {
        return blocks.entrySet().stream()
                .sorted(Comparator.comparing(entry -> sortKey(entry.getKey())))
                .map(Map.Entry::getValue)
                .toList();
    }

    private static SortKey sortKey(String path) {
        Family family = family(path);
        if (family == null) {
            return SortKey.fallback(path);
        }

        TextureShape textureShape = textureShape(family.remainder());
        return new SortKey(
                MATERIALS.indexOf(family.material()),
                family.material(),
                textureShape.textureOrder(),
                textureShape.textureName(),
                textureShape.variantOrder(),
                textureShape.ageOrder(),
                SHAPE_ORDER.getOrDefault(textureShape.shapeName(), Integer.MAX_VALUE),
                textureShape.shapeName(),
                path
        );
    }

    private static TextureShape textureShape(String remainder) {
        if (remainder.contains("_weave_")) {
            return weaveTextureShape(remainder);
        }

        boolean aged = remainder.startsWith("aged_") || remainder.endsWith("_aged");
        String base = remainder.startsWith("aged_")
                ? remainder.substring("aged_".length())
                : aged ? remainder.substring(0, remainder.length() - "_aged".length()) : remainder;
        int ageOrder = aged ? 1 : 0;

        if (base.startsWith("block_")) {
            String decoration = base.substring("block_".length());
            Integer decorationOrder = DECORATION_ORDER.get(decoration);
            if (decorationOrder != null) {
                return new TextureShape(45, "decorated", decorationOrder, ageOrder, "block");
            }
        }
        if (base.equals("rock") || base.startsWith("rock_")) {
            return new TextureShape(5, "rock", 0, ageOrder, afterPrefix(base, "rock", "block"));
        }
        if (base.equals("ashlar") || base.startsWith("ashlar_")) {
            return new TextureShape(10, "ashlar", 0, ageOrder, afterPrefix(base, "ashlar", "block"));
        }
        if (base.equals("rusticated") || base.startsWith("rusticated_")) {
            return new TextureShape(20, "rusticated", 0, ageOrder, afterPrefix(base, "rusticated", "block"));
        }
        if (base.equals("hewn") || base.startsWith("hewn_")) {
            return new TextureShape(30, "hewn", 0, ageOrder, afterPrefix(base, "hewn", "block"));
        }
        if (base.equals("herringbone_grout") || base.startsWith("herringbone_grout_")) {
            return new TextureShape(40, "herringbone_grout", 0, ageOrder,
                    afterPrefix(base, "herringbone_grout", "block"));
        }
        if (base.equals("herringbone_bronze") || base.startsWith("herringbone_bronze_")) {
            return new TextureShape(41, "herringbone_bronze", 0, ageOrder,
                    afterPrefix(base, "herringbone_bronze", "block"));
        }
        return new TextureShape(0, "base", 0, ageOrder, base);
    }

    private static TextureShape weaveTextureShape(String remainder) {
        int weaveIndex = remainder.indexOf("_weave_");
        String otherTexture = remainder.substring(0, weaveIndex);
        String afterWeave = remainder.substring(weaveIndex + "_weave_".length());
        String variant = firstPart(afterWeave);
        String shape = afterWeave.equals(variant) ? "block" : afterWeave.substring(variant.length() + 1);
        int variantOrder = switch (variant) {
            case "bronze" -> 0;
            case "grout" -> 1;
            default -> Integer.MAX_VALUE;
        };
        return new TextureShape(60, "weave_" + otherTexture, variantOrder, 0, shape);
    }

    private static Family family(String path) {
        for (String material : MATERIALS) {
            if (path.equals(material)) {
                return new Family(material, "block");
            }
            if (path.startsWith(material + "_")) {
                return new Family(material, path.substring(material.length() + 1));
            }
        }
        return null;
    }

    private static String afterPrefix(String value, String prefix, String fallbackShape) {
        return value.equals(prefix) ? fallbackShape : value.substring(prefix.length() + 1);
    }

    private static String firstPart(String value) {
        int index = value.indexOf('_');
        return index < 0 ? value : value.substring(0, index);
    }

    private record Family(String material, String remainder) {
    }

    private record TextureShape(int textureOrder, String textureName, int variantOrder, int ageOrder,
                                String shapeName) {
    }

    private record SortKey(int materialOrder, String material, int textureOrder, String textureName,
                           int variantOrder, int ageOrder, int shapeOrder, String shapeName,
                           String path) implements Comparable<SortKey> {
        private static SortKey fallback(String path) {
            return new SortKey(Integer.MAX_VALUE, "", Integer.MAX_VALUE, "", Integer.MAX_VALUE,
                    Integer.MAX_VALUE, Integer.MAX_VALUE, "", path);
        }

        @Override
        public int compareTo(SortKey other) {
            return Comparator.comparingInt(SortKey::materialOrder)
                    .thenComparing(SortKey::material)
                    .thenComparingInt(SortKey::textureOrder)
                    .thenComparing(SortKey::textureName)
                    .thenComparingInt(SortKey::variantOrder)
                    .thenComparingInt(SortKey::ageOrder)
                    .thenComparingInt(SortKey::shapeOrder)
                    .thenComparing(SortKey::shapeName)
                    .thenComparing(SortKey::path)
                    .compare(this, other);
        }
    }
}
