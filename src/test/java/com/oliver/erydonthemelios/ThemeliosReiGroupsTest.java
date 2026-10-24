package com.oliver.erydonthemelios;

import com.oliver.erydonthemelios.compat.rei.ThemeliosReiGroup;
import com.oliver.erydonthemelios.migration.ThemeliosIdMigration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThemeliosReiGroupsTest {
    @Test
    void everyRegisteredShapeBelongsToExactlyOneExpectedGroup() {
        Map<ThemeliosReiGroup, List<List<String>>> families = Map.of(
                ThemeliosReiGroup.SLABS, List.of(ModBlockIds.SLABS),
                ThemeliosReiGroup.STAIRS, List.of(ModBlockIds.STAIRS),
                ThemeliosReiGroup.WALLS, List.of(ModBlockIds.WALLS),
                ThemeliosReiGroup.LAYERS, List.of(ModBlockIds.LAYERS, ModBlockIds.VERTICAL_LAYERS),
                ThemeliosReiGroup.SLICES_AND_POSTS, List.of(
                        ModBlockIds.HORIZONTAL_SLICES, ModBlockIds.VERTICAL_SLICES, ModBlockIds.POSTS),
                ThemeliosReiGroup.CYLINDERS, List.of(ModBlockIds.CYLINDER_SMALLS, ModBlockIds.CYLINDER_LARGES),
                ThemeliosReiGroup.CORNICES, List.of(ModBlockIds.CORNICE_SMALLS, ModBlockIds.CORNICE_LARGES));

        families.forEach((expected, lists) -> lists.stream().flatMap(List::stream).forEach(registeredPath -> {
            String canonicalPath = ThemeliosIdMigration.canonicalPath(registeredPath);
            assertEquals(List.of(expected), Arrays.stream(ThemeliosReiGroup.values())
                    .filter(group -> group.matches(canonicalPath)).toList(), canonicalPath);
        }));
    }

    @Test
    void fullBlocksStayVisibleAndSubsetPathsRemainSeparateFromErydon() {
        for (String registeredPath : ModBlockIds.FULL_BLOCKS) {
            String canonicalPath = ThemeliosIdMigration.canonicalPath(registeredPath);
            assertTrue(Arrays.stream(ThemeliosReiGroup.values()).noneMatch(group -> group.matches(canonicalPath)),
                    canonicalPath);
        }
        assertEquals(7, Arrays.stream(ThemeliosReiGroup.values()).map(ThemeliosReiGroup::id).distinct().count());
        assertTrue(Arrays.stream(ThemeliosReiGroup.values())
                .allMatch(group -> group.subsetPath().startsWith("ERYDON Themelios/")));
    }
}
