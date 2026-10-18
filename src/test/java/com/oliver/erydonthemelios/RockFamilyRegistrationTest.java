package com.oliver.erydonthemelios;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockFamilyRegistrationTest {
    private static final List<String> MATERIALS = List.of(
            "aganite", "aterzon", "borealis", "brectite", "calacattum", "chalstrom", "chrysonyx",
            "etruscus", "gelastrum", "glacium", "hesperion", "imperium", "kelastrion", "kylorion",
            "latmion", "laurentium", "mielonyx", "nerium", "noxoplis", "porphyros", "portorium",
            "psamatheon", "rosinium", "sanguenite", "selenephos", "solistra", "striatus"
    );

    @Test
    void registersEveryRockMaterialAcrossAllThemeliosShapes() {
        assertRockShape(ModBlockIds.FULL_BLOCKS, "block");
        assertRockShape(ModBlockIds.SLABS, "slab");
        assertRockShape(ModBlockIds.STAIRS, "stairs");
        assertRockShape(ModBlockIds.WALLS, "wall");
        assertRockShape(ModBlockIds.POSTS, "post");
        assertRockShape(ModBlockIds.HORIZONTAL_SLICES, "slice_horizontal");
        assertRockShape(ModBlockIds.VERTICAL_SLICES, "slice_vertical");
        assertRockShape(ModBlockIds.LAYERS, "layer");
        assertRockShape(ModBlockIds.VERTICAL_LAYERS, "layer_vertical");
        assertRockShape(ModBlockIds.CYLINDER_SMALLS, "cylinder_small");
        assertRockShape(ModBlockIds.CYLINDER_LARGES, "cylinder_large");

        long distinctRockIds = Stream.of(
                        ModBlockIds.FULL_BLOCKS,
                        ModBlockIds.SLABS,
                        ModBlockIds.STAIRS,
                        ModBlockIds.WALLS,
                        ModBlockIds.POSTS,
                        ModBlockIds.HORIZONTAL_SLICES,
                        ModBlockIds.VERTICAL_SLICES,
                        ModBlockIds.LAYERS,
                        ModBlockIds.VERTICAL_LAYERS,
                        ModBlockIds.CYLINDER_SMALLS,
                        ModBlockIds.CYLINDER_LARGES
                )
                .flatMap(List::stream)
                .filter(id -> id.contains("_rock_"))
                .distinct()
                .count();
        assertEquals(297, distinctRockIds);
    }

    private static void assertRockShape(List<String> ids, String shape) {
        List<String> rockIds = ids.stream()
                .filter(id -> id.contains("_rock_"))
                .toList();
        assertEquals(27, rockIds.size(), shape);
        for (String material : MATERIALS) {
            assertTrue(
                    rockIds.contains(material + "_rock_" + shape),
                    () -> "Missing " + material + " Rock " + shape
            );
        }
    }
}
