package com.oliver.erydonthemelios.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class CylinderCapGeometryTest {
    @Test
    void bothMastersHaveEightDisjointCapQuadsWithTheSameStoneFootprint() throws Exception {
        for (String size : List.of("small", "large")) {
            List<List<CylinderCapGeometry.Point>> strips = loadStrips(size);
            Area authored = union(strips);
            for (boolean up : List.of(false, true)) {
                List<List<CylinderCapGeometry.Point>> caps = CylinderCapGeometry.retile(strips, up);
                assertEquals(8, caps.size(), size);
                for (int i = 0; i < caps.size(); i++) {
                    assertEquals(4, caps.get(i).size());
                    assertEquals(up, signedArea(caps.get(i)) < 0, "outward cap winding");
                    for (int j = i + 1; j < caps.size(); j++) {
                        Area overlap = area(caps.get(i));
                        overlap.intersect(area(caps.get(j)));
                        assertEquals(0, surface(overlap), 1.0e-10, "no coplanar overlapping area");
                    }
                }
                Area difference = union(caps);
                difference.exclusiveOr(authored);
                // Source rounding leaves microscopic paired corners; the silhouette agrees
                // to well below one pixel even with the optional 64x stone artwork.
                assertTrue(surface(difference) < 0.00002, size + " footprint changed");
                double drawnBefore = strips.stream().mapToDouble(p -> Math.abs(signedArea(p))).sum();
                double drawnAfter = caps.stream().mapToDouble(p -> Math.abs(signedArea(p))).sum();
                assertTrue(drawnAfter < drawnBefore * 0.501, "cap overdraw should halve");
            }
        }
    }

    @Test
    void unfamiliarReplacementGeometryIsLeftAlone() {
        assertTrue(CylinderCapGeometry.retile(List.of(List.of(
                new CylinderCapGeometry.Point(0, 0),
                new CylinderCapGeometry.Point(1, 0),
                new CylinderCapGeometry.Point(1, 1),
                new CylinderCapGeometry.Point(0, 1))), true).isEmpty());
    }

    @Test
    void bakedCapsUseOneUvProjectionAndPreserveTheSideQuads() throws Exception {
        var id = net.minecraft.util.Identifier.of("erydon", "block/aganite_block");
        var contents = new net.minecraft.client.texture.SpriteContents(id,
                new net.minecraft.client.texture.SpriteDimensions(16, 16),
                new net.minecraft.client.texture.NativeImage(16, 16, false),
                net.minecraft.resource.metadata.ResourceMetadata.NONE);
        var sprite = new TestSprite(id, contents);
        for (String size : List.of("small", "large")) {
            var faces = new java.util.EnumMap<net.minecraft.util.math.Direction,
                    List<net.minecraft.client.render.model.BakedQuad>>(net.minecraft.util.math.Direction.class);
            for (var face : net.minecraft.util.math.Direction.values()) faces.put(face, new ArrayList<>());
            for (var polygon : loadStrips(size)) {
                for (var face : List.of(net.minecraft.util.math.Direction.UP, net.minecraft.util.math.Direction.DOWN)) {
                    int[] data = new int[32];
                    for (int i = 0; i < 4; i++) {
                        data[i * 8] = Float.floatToRawIntBits(polygon.get(i).x());
                        data[i * 8 + 1] = Float.floatToRawIntBits(face == net.minecraft.util.math.Direction.UP ? 1 : 0);
                        data[i * 8 + 2] = Float.floatToRawIntBits(polygon.get(i).z());
                        data[i * 8 + 3] = -1;
                    }
                    faces.get(face).add(new net.minecraft.client.render.model.BakedQuad(data, -1, face, sprite, true));
                }
            }
            var side = new net.minecraft.client.render.model.BakedQuad(new int[32], -1,
                    net.minecraft.util.math.Direction.NORTH, sprite, true);
            faces.get(net.minecraft.util.math.Direction.NORTH).add(side);
            var original = new net.minecraft.client.render.model.BasicBakedModel(List.of(), faces,
                    true, true, true, sprite, null, null);
            var modelId = net.minecraft.util.Identifier.of("erydon", "block/cylinder/aganite_cylinder_" + size);
            var repaired = CylinderCapGeometry.repair(modelId, original);
            assertNotSame(original, repaired);
            var random = net.minecraft.util.math.random.Random.create(0);
            assertSame(side, repaired.getQuads(null, net.minecraft.util.math.Direction.NORTH, random).get(0));
            for (var face : List.of(net.minecraft.util.math.Direction.UP, net.minecraft.util.math.Direction.DOWN)) {
                var quads = repaired.getQuads(null, face, random);
                assertEquals(8, quads.size());
                for (var quad : quads) {
                    int[] data = quad.getVertexData();
                    for (int i = 0; i < 4; i++) {
                        float x = Float.intBitsToFloat(data[i * 8]);
                        float z = Float.intBitsToFloat(data[i * 8 + 2]);
                        assertEquals(face == net.minecraft.util.math.Direction.UP ? 1 : 0,
                                Float.intBitsToFloat(data[i * 8 + 1]));
                        assertEquals(sprite.getFrameU(16 * x), Float.intBitsToFloat(data[i * 8 + 4]), 0.000001);
                        assertEquals(sprite.getFrameV(16 * (face == net.minecraft.util.math.Direction.UP ? z : 1 - z)),
                                Float.intBitsToFloat(data[i * 8 + 5]), 0.000001);
                    }
                }
                assertEquals(0, original.getQuads(null, face, random).get(0).getVertexData()[4],
                        "original cached model must remain untouched");
            }
        }
    }

    private static final class TestSprite extends net.minecraft.client.texture.Sprite {
        TestSprite(net.minecraft.util.Identifier id, net.minecraft.client.texture.SpriteContents contents) {
            super(id, contents, 64, 64, 16, 16);
        }
    }

    private static List<List<CylinderCapGeometry.Point>> loadStrips(String size) throws Exception {
        JsonObject model = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/erydon/models/block/cylinder/cylinder_" + size + ".json")))
                .getAsJsonObject();
        List<List<CylinderCapGeometry.Point>> strips = new ArrayList<>();
        for (var entry : model.getAsJsonArray("elements")) {
            JsonObject element = entry.getAsJsonObject();
            var from = element.getAsJsonArray("from");
            var to = element.getAsJsonArray("to");
            double angle = Math.toRadians(element.getAsJsonObject("rotation").get("angle").getAsDouble());
            double c = Math.cos(angle), s = Math.sin(angle);
            List<CylinderCapGeometry.Point> points = new ArrayList<>();
            for (int[] corner : new int[][]{{0, 0}, {1, 0}, {1, 1}, {0, 1}}) {
                double x = (corner[0] == 0 ? from : to).get(0).getAsDouble() - 8;
                double z = (corner[1] == 0 ? from : to).get(2).getAsDouble() - 8;
                points.add(new CylinderCapGeometry.Point(
                        (float) ((8 + x * c + z * s) / 16),
                        (float) ((8 - x * s + z * c) / 16)));
            }
            strips.add(points);
        }
        return strips;
    }

    private static Area area(List<CylinderCapGeometry.Point> polygon) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(polygon.get(0).x(), polygon.get(0).z());
        for (int i = 1; i < polygon.size(); i++) path.lineTo(polygon.get(i).x(), polygon.get(i).z());
        path.closePath();
        return new Area(path);
    }

    private static Area union(List<List<CylinderCapGeometry.Point>> polygons) {
        Area result = new Area();
        polygons.forEach(polygon -> result.add(area(polygon)));
        return result;
    }

    private static double signedArea(List<CylinderCapGeometry.Point> points) {
        double result = 0;
        for (int i = 0; i < points.size(); i++) {
            var a = points.get(i);
            var b = points.get((i + 1) % points.size());
            result += (double) a.x() * b.z() - (double) b.x() * a.z();
        }
        return result / 2;
    }

    private static double surface(Area area) {
        PathIterator iterator = area.getPathIterator(null);
        double[] coordinates = new double[6];
        double firstX = 0, firstZ = 0, lastX = 0, lastZ = 0, sum = 0;
        while (!iterator.isDone()) {
            int kind = iterator.currentSegment(coordinates);
            if (kind == PathIterator.SEG_MOVETO) {
                firstX = lastX = coordinates[0];
                firstZ = lastZ = coordinates[1];
            } else if (kind == PathIterator.SEG_LINETO) {
                sum += lastX * coordinates[1] - coordinates[0] * lastZ;
                lastX = coordinates[0];
                lastZ = coordinates[1];
            } else if (kind == PathIterator.SEG_CLOSE) {
                sum += lastX * firstZ - firstX * lastZ;
            } else fail("Caps must contain only straight edges");
            iterator.next();
        }
        return Math.abs(sum / 2);
    }
}
