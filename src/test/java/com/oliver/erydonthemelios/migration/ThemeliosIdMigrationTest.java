package com.oliver.erydonthemelios.migration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeliosIdMigrationTest {
    @Test
    void loadsTheCompleteApprovedPublishedMap() {
        assertEquals(861, ThemeliosIdMigration.entries().size());
        assertEquals(861, ThemeliosIdMigration.aliases().size());
        assertEquals(48, ThemeliosIdMigration.aliases().stream()
                .filter(entry -> entry.oldId().getPath().contains("quatrefoil"))
                .count());
        assertEquals(54, ThemeliosIdMigration.aliases().stream()
                .filter(entry -> entry.oldId().getPath().contains("cylinder_"))
                .count());
        assertFalse(ThemeliosIdMigration.entries().stream()
                .anyMatch(entry -> entry.canonicalId().getPath().contains("column_round")));
    }

    @Test
    void canonicalisesRepresentativeAgedSliceAndInlayIds() {
        assertEquals("aganite_aged_block",
                ThemeliosIdMigration.canonicalPath("aganite_block_aged"));
        assertEquals("aganite_aged_slice_horizontal",
                ThemeliosIdMigration.canonicalPath("aganite_horizontal_slice_aged"));
        assertEquals("aganite_ashlar_slice_vertical",
                ThemeliosIdMigration.canonicalPath("aganite_ashlar_vertical_slice"));
        assertEquals("aganite_guilloche_silver_block",
                ThemeliosIdMigration.canonicalPath("aganite_block_silverguilloche"));
        assertEquals("aganite_rosette_bronze_block",
                ThemeliosIdMigration.canonicalPath("aganite_block_bronzerose"));
        assertEquals("aganite_aged_cylinder_small",
                ThemeliosIdMigration.canonicalPath("aganite_cylinder_small_aged"));
    }

    @Test
    void keepsPublishedInternalResourcesStable() {
        assertEquals("aganite_block_aged",
                ThemeliosIdMigration.legacyResourcePath("aganite_aged_block"));
        assertEquals("aganite_ashlar_vertical_slice",
                ThemeliosIdMigration.legacyResourcePath("aganite_ashlar_slice_vertical"));
        assertEquals("aganite_block_bronzequatrefoil",
                ThemeliosIdMigration.legacyResourcePath("aganite_quatrefoil_bronze_block"));
    }

    @Test
    void exposesOldAndNewSearchVocabulary() {
        List<String> motif = ThemeliosIdMigration.searchTermsForCanonicalPath(
                "aganite_guilloche_silver_block");
        assertTrue(motif.contains("themelios:aganite_block_silverguilloche"));
        assertTrue(motif.contains("themelios:aganite_guilloche_silver_block"));
        assertTrue(motif.contains("guilloche"));
        assertTrue(motif.contains("byzantine"));

        List<String> rosette = ThemeliosIdMigration.searchTermsForCanonicalPath(
                "aganite_rosette_bronze_block");
        assertTrue(rosette.contains("rose"));
        assertTrue(rosette.contains("rosette"));
        assertTrue(rosette.contains("inlay"));
    }
}
