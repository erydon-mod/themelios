package com.oliver.erydonthemelios;

import com.oliver.erydonthemelios.migration.ThemeliosIdMigration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registered shape siblings of each canonical full block, without initializing block registries. */
public final class ThemeliosShapeFamilies {
    private static final Map<String, List<String>> SHAPES_BY_FULL_BLOCK = createFamilies();

    private ThemeliosShapeFamilies() {
    }

    public static List<String> shapesForFullBlock(String canonicalFullBlockPath) {
        return SHAPES_BY_FULL_BLOCK.getOrDefault(canonicalFullBlockPath, List.of());
    }

    private static Map<String, List<String>> createFamilies() {
        Map<String, List<String>> families = new LinkedHashMap<>();
        for (String fullBlock : ModBlockIds.FULL_BLOCKS) {
            families.put(ThemeliosIdMigration.canonicalPath(fullBlock), new ArrayList<>());
        }
        addShapes(families, ModBlockIds.SLABS, "slab");
        addShapes(families, ModBlockIds.STAIRS, "stairs");
        addShapes(families, ModBlockIds.WALLS, "wall");
        addShapes(families, ModBlockIds.POSTS, "post");
        addShapes(families, ModBlockIds.HORIZONTAL_SLICES, "slice_horizontal");
        addShapes(families, ModBlockIds.VERTICAL_SLICES, "slice_vertical");
        addShapes(families, ModBlockIds.LAYERS, "layer");
        addShapes(families, ModBlockIds.VERTICAL_LAYERS, "layer_vertical");
        addShapes(families, ModBlockIds.CYLINDER_SMALLS, "cylinder_small");
        addShapes(families, ModBlockIds.CYLINDER_LARGES, "cylinder_large");
        addShapes(families, ModBlockIds.CORNICE_SMALLS, "cornice_small");
        addShapes(families, ModBlockIds.CORNICE_LARGES, "cornice_large");
        Map<String, List<String>> immutable = new LinkedHashMap<>();
        families.forEach((fullBlock, shapes) -> immutable.put(fullBlock, List.copyOf(shapes)));
        return Map.copyOf(immutable);
    }

    private static void addShapes(Map<String, List<String>> families,
                                  List<String> registeredPaths,
                                  String form) {
        String suffix = "_" + form;
        for (String registeredPath : registeredPaths) {
            String canonical = ThemeliosIdMigration.canonicalPath(registeredPath);
            if (!canonical.endsWith(suffix)) {
                continue;
            }
            String fullBlock = canonical.substring(0, canonical.length() - suffix.length()) + "_block";
            List<String> family = families.get(fullBlock);
            if (family != null) {
                family.add(canonical);
            }
        }
    }
}
