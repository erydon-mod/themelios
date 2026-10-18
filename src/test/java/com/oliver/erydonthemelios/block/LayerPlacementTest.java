package com.oliver.erydonthemelios.block;

import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LayerPlacementTest {
    @Test
    void horizontalLayersGrowOnlyFromTheirExposedTopOrUnderside() {
        assertMergeFaces(LayerPlacement.layerFaces(false), Set.of(Direction.UP));
        assertMergeFaces(LayerPlacement.layerFaces(true), Set.of(Direction.DOWN));
    }

    @Test
    void verticalLayersGrowFromTheFrontAndAllowSeparateBlocksOnTheirEdges() {
        assertMergeFaces(LayerPlacement.verticalLayerFaces(Direction.NORTH), Set.of(Direction.SOUTH));
        assertMergeFaces(LayerPlacement.verticalLayerFaces(Direction.EAST), Set.of(Direction.WEST));
        assertMergeFaces(LayerPlacement.verticalLayerFaces(Direction.SOUTH), Set.of(Direction.NORTH));
        assertMergeFaces(LayerPlacement.verticalLayerFaces(Direction.WEST), Set.of(Direction.EAST));
    }

    @Test
    void horizontalSlicesGrowFromFrontAndExposedTopForEveryFacingAndAnchor() {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            assertMergeFaces(LayerPlacement.horizontalSliceFaces(facing, false),
                    Set.of(facing.getOpposite(), Direction.UP));
            assertMergeFaces(LayerPlacement.horizontalSliceFaces(facing, true),
                    Set.of(facing.getOpposite(), Direction.DOWN));
        }
    }

    @Test
    void cornerSlicesGrowOnlyOnTheirTwoOutwardFaces() {
        assertMergeFaces(LayerPlacement.verticalSliceFaces(Direction.NORTH),
                Set.of(Direction.SOUTH, Direction.EAST));
        assertMergeFaces(LayerPlacement.verticalSliceFaces(Direction.EAST),
                Set.of(Direction.SOUTH, Direction.WEST));
        assertMergeFaces(LayerPlacement.verticalSliceFaces(Direction.SOUTH),
                Set.of(Direction.NORTH, Direction.WEST));
        assertMergeFaces(LayerPlacement.verticalSliceFaces(Direction.WEST),
                Set.of(Direction.NORTH, Direction.EAST));
    }

    @Test
    void postEndCapsExtendThePostInsteadOfThickeningIt() {
        assertMergeFaces(LayerPlacement.postFaces(Direction.Axis.Y),
                Set.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        assertMergeFaces(LayerPlacement.postFaces(Direction.Axis.X),
                Set.of(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH));
        assertMergeFaces(LayerPlacement.postFaces(Direction.Axis.Z),
                Set.of(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST));
    }

    @Test
    void crouchingAndRedirectedPlacementNeverMergeAnExistingStack() {
        for (int faces : allGrowthMasks()) {
            for (int layers = 1; layers <= 8; layers++) {
                for (Direction side : Direction.values()) {
                    assertFalse(LayerPlacement.canMerge(layers, true, true, side, faces));
                    assertFalse(LayerPlacement.canMerge(layers, false, false, side, faces));
                }
            }
        }
    }

    @Test
    void onlyIncompleteStacksAcceptAnotherLayer() {
        for (int layers = 1; layers < 8; layers++) {
            assertTrue(LayerPlacement.canMerge(layers, false, true,
                    Direction.UP, LayerPlacement.layerFaces(false)));
        }
        assertFalse(LayerPlacement.canMerge(8, false, true,
                Direction.UP, LayerPlacement.layerFaces(false)));
    }

    private static void assertMergeFaces(int faces, Set<Direction> expected) {
        for (int layers = 1; layers <= 8; layers++) {
            Set<Direction> actual = EnumSet.noneOf(Direction.class);
            for (Direction side : Direction.values()) {
                if (LayerPlacement.canMerge(layers, false, true, side, faces)) {
                    actual.add(side);
                }
            }
            assertEquals(layers < 8 ? expected : Set.of(), actual, "Layer count " + layers);
        }
    }

    private static List<Integer> allGrowthMasks() {
        return List.of(
                LayerPlacement.layerFaces(false), LayerPlacement.layerFaces(true),
                LayerPlacement.verticalLayerFaces(Direction.NORTH),
                LayerPlacement.horizontalSliceFaces(Direction.NORTH, false),
                LayerPlacement.horizontalSliceFaces(Direction.NORTH, true),
                LayerPlacement.verticalSliceFaces(Direction.NORTH),
                LayerPlacement.postFaces(Direction.Axis.X),
                LayerPlacement.postFaces(Direction.Axis.Y),
                LayerPlacement.postFaces(Direction.Axis.Z));
    }
}
