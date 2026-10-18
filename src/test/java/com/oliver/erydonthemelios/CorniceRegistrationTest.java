package com.oliver.erydonthemelios;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class CorniceRegistrationTest {
    @Test
    void registersBothSizesForTheErydonCorniceTextureSet() {
        assertEquals(54, ModBlockIds.CORNICE_SMALLS.size());
        assertEquals(54, ModBlockIds.CORNICE_LARGES.size());
        assertEquals(108, Stream.concat(
                        ModBlockIds.CORNICE_SMALLS.stream(),
                        ModBlockIds.CORNICE_LARGES.stream())
                .distinct()
                .count());
    }

    @Test
    void usesCanonicalAgedIdOrder() {
        assertFalse(Stream.concat(
                        ModBlockIds.CORNICE_SMALLS.stream(),
                        ModBlockIds.CORNICE_LARGES.stream())
                .anyMatch(name -> name.matches(".*_cornice_(small|large)_aged")));
    }
}
