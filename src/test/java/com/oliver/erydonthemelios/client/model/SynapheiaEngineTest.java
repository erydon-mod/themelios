package com.oliver.erydonthemelios.client.model;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SynapheiaEngineTest {
    @Test
    void signedOutOfBoundsGeometrySplitsAtEveryWorldCell() {
        List<SpiralStairCtmGeometry.Vertex> vertices = List.of(
                vertex(-0.5F, 0.25F, -0.5F),
                vertex(1.5F, 0.25F, -0.5F),
                vertex(1.5F, 0.25F, 1.5F),
                vertex(-0.5F, 0.25F, 1.5F)
        );

        List<SpiralStairCtmGeometry.Fragment> fragments =
                SpiralStairCtmGeometry.split(Direction.UP, vertices);

        assertEquals(9, fragments.size());
        assertEquals(Set.of(-1, 0, 1), fragments.stream()
                .map(SpiralStairCtmGeometry.Fragment::cellS)
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of(-1, 0, 1), fragments.stream()
                .map(SpiralStairCtmGeometry.Fragment::cellT)
                .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void trianglePaddingSurvivesNormalAndFlippedRendererOrders() {
        TangentVertex first = new TangentVertex(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        TangentVertex second = new TangentVertex(1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
        TangentVertex third = new TangentVertex(0.0F, 1.0F, 0.0F, 0.0F, 1.0F);

        List<TangentVertex> padded =
                SynapheiaRepeatBakedModel.padTriangle(first, second, third);

        assertEquals(List.of(first, second, third, first), padded);
        assertValidTangentTriangle(padded, 0, 1, 2);
        assertValidTangentTriangle(padded, 1, 2, 3);
    }

    @Test
    void legacyAgedRulesAlsoIndexCanonicalBlocks() {
        List<Identifier> tiles = IntStream.range(0, 36)
                .mapToObj(index -> Identifier.of("minecraft", "optifine/ctm/aganite/" + index))
                .toList();
        Identifier legacy = Identifier.of("themelios", "aganite_block_aged");
        Identifier canonical = Identifier.of("themelios", "aganite_aged_block");
        SynapheiaManifest.Rule rule = new SynapheiaManifest.Rule(
                Identifier.of("minecraft", "optifine/ctm/themelios/aganite.properties"),
                "test", SynapheiaManifest.Method.REPEAT, tiles,
                Set.of(Direction.values()), Set.of(legacy), Set.of(), true, 10,
                SynapheiaManifest.OverlayLayer.CUTOUT);
        SynapheiaManifest.Prepared prepared =
                new SynapheiaManifest.Prepared(List.of(rule), "test", 1, 1, 0, 1L);

        SynapheiaService.Snapshot first = SynapheiaService.publish(prepared);
        SynapheiaService.Snapshot second = SynapheiaService.publish(prepared);

        assertNotEquals(first.generation(), second.generation());
        assertEquals(List.of(rule), second.rulesFor(legacy));
        assertEquals(List.of(rule), second.rulesFor(canonical));
    }

    @Test
    void connectedTextureSelectorCoversAllFortySevenTiles() {
        Set<Integer> tiles = IntStream.range(0, 256)
                .filter(SynapheiaEngineTest::isCanonicalMask)
                .map(SynapheiaRepeatBakedModel::connectedTileIndex)
                .boxed()
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(IntStream.range(0, 47).boxed()
                .collect(java.util.stream.Collectors.toSet()), tiles);
    }

    @Test
    void everyProductionRuleUsesASupportedSynapheiaMethod() throws Exception {
        Path root = Path.of("src/main/resources/assets/minecraft/optifine/ctm");
        int repeat = 0;
        int overlay = 0;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".properties")).toList()) {
                Properties properties = new Properties();
                try (InputStream input = Files.newInputStream(path)) {
                    properties.load(input);
                }
                Set<Identifier> blocks =
                        SynapheiaManifest.parseBlocks(properties.getProperty("matchBlocks"));
                if (blocks.isEmpty()) {
                    continue;
                }
                String relative = Path.of("src/main/resources/assets/minecraft")
                        .relativize(path).toString().replace('\\', '/');
                SynapheiaManifest.Rule rule = SynapheiaManifest.parseRule(
                        Identifier.of("minecraft", relative), "production-test", properties, blocks);
                if (rule.method() == SynapheiaManifest.Method.REPEAT) {
                    repeat++;
                } else {
                    overlay++;
                }
            }
        }
        assertEquals(288, repeat);
        assertEquals(192, overlay);
    }

    @Test
    void repeatGridPreservesAllSixFaceOrientationsAtSignedCoordinates() {
        Set<Integer> indices = java.util.Arrays.stream(Direction.values())
                .map(face -> SynapheiaTileGrid.repeatTileIndex(-7, 31, -11, face))
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(indices.size() >= 4);
        assertTrue(indices.stream().allMatch(index -> index >= 0 && index < 36));
    }

    private static SpiralStairCtmGeometry.Vertex vertex(float x, float y, float z) {
        return new SpiralStairCtmGeometry.Vertex(
                x, y, z, -1, 0, true, 0.0F, 1.0F, 0.0F, Float.NaN, Float.NaN);
    }

    private static void assertValidTangentTriangle(List<TangentVertex> vertices,
                                                   int firstIndex,
                                                   int secondIndex,
                                                   int thirdIndex) {
        TangentVertex first = vertices.get(firstIndex);
        TangentVertex second = vertices.get(secondIndex);
        TangentVertex third = vertices.get(thirdIndex);
        float firstEdgeX = second.x - first.x;
        float firstEdgeY = second.y - first.y;
        float firstEdgeZ = second.z - first.z;
        float secondEdgeX = third.x - first.x;
        float secondEdgeY = third.y - first.y;
        float secondEdgeZ = third.z - first.z;
        float crossX = firstEdgeY * secondEdgeZ - firstEdgeZ * secondEdgeY;
        float crossY = firstEdgeZ * secondEdgeX - firstEdgeX * secondEdgeZ;
        float crossZ = firstEdgeX * secondEdgeY - firstEdgeY * secondEdgeX;
        float crossLengthSquared = crossX * crossX + crossY * crossY + crossZ * crossZ;
        float uvDeterminant = (second.u - first.u) * (third.v - first.v)
                - (second.v - first.v) * (third.u - first.u);

        assertTrue(crossLengthSquared > 0.000000001F);
        assertTrue(Math.abs(uvDeterminant) > 0.000000001F);
    }

    private static boolean isCanonicalMask(int mask) {
        for (int corner = 0; corner < 4; corner++) {
            int cornerBit = 1 << (corner * 2 + 1);
            int firstEdge = 1 << (corner * 2);
            int secondEdge = 1 << (((corner + 1) & 3) * 2);
            if ((mask & cornerBit) != 0
                    && ((mask & firstEdge) == 0 || (mask & secondEdge) == 0)) {
                return false;
            }
        }
        return true;
    }

    private record TangentVertex(float x, float y, float z, float u, float v) {
    }
}
