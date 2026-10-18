package com.oliver.erydonthemelios.client.model;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the shared ERYDON world texture phase independently of authored model UVs. */
final class SynapheiaWorldAlignmentTest {
    @Test
    void signedPositionSelectsTheExactSharedTileOnEveryFace() {
        BlockPos pos = new BlockPos(-7, 31, -11);
        assertEquals(29, tile(pos, Direction.DOWN));
        assertEquals(11, tile(pos, Direction.UP));
        assertEquals(30, tile(pos, Direction.NORTH));
        assertEquals(35, tile(pos, Direction.SOUTH));
        assertEquals(31, tile(pos, Direction.WEST));
        assertEquals(34, tile(pos, Direction.EAST));
    }

    @Test
    void bottomTopAndDoubleSlabsKeepTheirWorldCropOnEveryFace() {
        for (BlockPos pos : List.of(BlockPos.ORIGIN,
                new BlockPos(-7, 31, -11), new BlockPos(8, -19, 13))) {
            for (Direction face : Direction.values()) {
                for (float low : new float[]{0.0F, 0.5F}) {
                    float high = low + 0.5F;
                    List<SpiralStairCtmGeometry.Vertex> vertices = face.getAxis() == Direction.Axis.Y
                            ? rectangle(face, high, 0, 1, 0, 1)
                            : rectangle(face, 0, 0, 1, low, high);
                    var fragments = SpiralStairCtmGeometry.split(face, vertices);
                    assertEquals(1, fragments.size(), "a slab face stays in one texture cell");
                    assertWorldPhase(pos, face, fragments);
                    for (var vertex : fragments.get(0).vertices()) {
                        if (face.getAxis() != Direction.Axis.Y) {
                            float v = SpiralStairCtmGeometry.v(face, vertex);
                            assertTrue(v >= 1.0F - high && v <= 1.0F - low,
                                    "slab sides crop the shared tile instead of stretching it");
                        }
                    }
                }
                assertWorldPhase(pos, face, SpiralStairCtmGeometry.split(face,
                        rectangle(face, 1, 0, 1, 0, 1)));
            }
        }
    }

    @Test
    void partialAndCrossCellSurfacesUseTheSamePhaseAtSignedCoordinates() {
        for (BlockPos pos : List.of(BlockPos.ORIGIN,
                new BlockPos(-13, -7, 11), new BlockPos(17, 19, -23))) {
            for (Direction face : Direction.values()) {
                for (List<SpiralStairCtmGeometry.Vertex> vertices : List.of(
                        rectangle(face, 0.5F, 0.125F, 0.875F, 0.25F, 0.75F),
                        rectangle(face, 0.5F, -0.5F, 1.5F, -0.5F, 1.5F))) {
                    var fragments = SpiralStairCtmGeometry.split(face, vertices);
                    assertTrue(!fragments.isEmpty());
                    assertWorldPhase(pos, face, fragments);
                }
            }
        }
    }

    private static void assertWorldPhase(BlockPos pos, Direction face,
                                         List<SpiralStairCtmGeometry.Fragment> fragments) {
        for (var fragment : fragments) {
            int tile = SynapheiaTileGrid.repeatTileIndex(
                    pos.getX() + SpiralStairCtmGeometry.offsetX(face, fragment),
                    pos.getY() + SpiralStairCtmGeometry.offsetY(face, fragment),
                    pos.getZ() + SpiralStairCtmGeometry.offsetZ(face, fragment), face);
            for (var cell : fragment.vertices()) {
                var vertex = cell.vertex();
                double x = pos.getX() + vertex.x();
                double y = pos.getY() + vertex.y();
                double z = pos.getZ() + vertex.z();
                // The shared sheet is six blocks wide/high. Side V starts at the
                // top of the owning world cell, including at negative coordinates.
                double expectedU = switch (face) {
                    case NORTH -> -x;
                    case SOUTH, UP, DOWN -> x;
                    case WEST -> z;
                    case EAST -> -z;
                };
                double expectedV = switch (face) {
                    case UP -> z;
                    case DOWN -> -z;
                    default -> 1.0D - y;
                };
                assertSamePhase(expectedU, tile % 6 + SpiralStairCtmGeometry.u(face, cell));
                assertSamePhase(expectedV, tile / 6 + SpiralStairCtmGeometry.v(face, cell));
            }
        }
    }

    private static void assertSamePhase(double expected, double actual) {
        double difference = Math.abs(mod6(expected) - mod6(actual));
        assertEquals(0.0D, Math.min(difference, 6.0D - difference), 0.00001D);
    }

    private static double mod6(double value) {
        return value - Math.floor(value / 6.0D) * 6.0D;
    }

    private static int tile(BlockPos pos, Direction face) {
        return SynapheiaTileGrid.repeatTileIndex(pos.getX(), pos.getY(), pos.getZ(), face);
    }

    private static List<SpiralStairCtmGeometry.Vertex> rectangle(
            Direction face, float height, float minS, float maxS, float minT, float maxT) {
        return List.of(vertex(face, height, minS, minT), vertex(face, height, maxS, minT),
                vertex(face, height, maxS, maxT), vertex(face, height, minS, maxT));
    }

    private static SpiralStairCtmGeometry.Vertex vertex(Direction face, float height, float s, float t) {
        float x = face.getAxis() == Direction.Axis.X ? height : s;
        float y = face.getAxis() == Direction.Axis.Y ? height : t;
        float z = face.getAxis() == Direction.Axis.X ? s
                : face.getAxis() == Direction.Axis.Y ? t : height;
        // Deliberately unrelated authored UVs: world alignment must come from position.
        return new SpiralStairCtmGeometry.Vertex(x, y, z, -1, 0, true,
                face.getOffsetX(), face.getOffsetY(), face.getOffsetZ(), 0.37F, 0.61F);
    }
}
