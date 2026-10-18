package com.oliver.erydonthemelios.client.model;

import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BasicBakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Repairs the shared cylinder caps once at bake time, with no extra rendered faces. */
final class CylinderCapGeometry {
    // The authored strips round coincident corners to four decimal model units.
    private static final float CORNER_EPSILON = 0.00001F;

    private CylinderCapGeometry() { }

    static BakedModel repair(Identifier id, BakedModel model) {
        if (!isCylinder(id) || !(model instanceof BasicBakedModel)) return model;
        Random random = Random.create(0);
        Map<Direction, List<BakedQuad>> faces = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) {
            List<BakedQuad> original = model.getQuads(null, face, random);
            faces.put(face, face.getAxis() == Direction.Axis.Y ? repairCap(original, face) : original);
        }
        return new BasicBakedModel(model.getQuads(null, null, random), faces,
                model.useAmbientOcclusion(), model.hasDepth(), model.isSideLit(),
                model.getParticleSprite(), model.getTransformation(), model.getOverrides());
    }

    static boolean isCylinder(Identifier id) {
        if (id == null) return false;
        String path = id.getPath();
        return (path.endsWith("cylinder_small") || path.endsWith("cylinder_large"))
                && ("themelios".equals(id.getNamespace())
                || ("erydon".equals(id.getNamespace()) && path.startsWith("block/cylinder/")));
    }

    private static List<BakedQuad> repairCap(List<BakedQuad> source, Direction face) {
        if (source.size() != 8) return source;
        List<List<Point>> polygons = new ArrayList<>();
        BakedQuad first = source.get(0);
        float height = face == Direction.UP ? 1.0F : 0.0F;
        for (BakedQuad quad : source) {
            if (quad.getFace() != face || quad.getSprite() != first.getSprite()) return source;
            int[] data = quad.getVertexData();
            if (data.length != 32) return source;
            List<Point> polygon = new ArrayList<>();
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * 8;
                if (Math.abs(Float.intBitsToFloat(data[offset + 1]) - height) > CORNER_EPSILON) {
                    return source;
                }
                polygon.add(new Point(Float.intBitsToFloat(data[offset]),
                        Float.intBitsToFloat(data[offset + 2])));
            }
            polygons.add(polygon);
        }
        List<List<Point>> caps = retile(polygons, face == Direction.UP);
        if (caps.isEmpty()) return source;
        List<BakedQuad> result = new ArrayList<>(8);
        Sprite sprite = first.getSprite();
        for (List<Point> cap : caps) {
            int[] data = first.getVertexData().clone();
            for (int vertex = 0; vertex < 4; vertex++) {
                Point point = cap.get(vertex);
                int offset = vertex * 8;
                data[offset] = Float.floatToRawIntBits(point.x());
                data[offset + 1] = Float.floatToRawIntBits(height);
                data[offset + 2] = Float.floatToRawIntBits(point.z());
                // One block-space projection for the entire cap; never shrink each patch.
                data[offset + 4] = Float.floatToRawIntBits(sprite.getFrameU(point.x() * 16.0F));
                data[offset + 5] = Float.floatToRawIntBits(sprite.getFrameV(
                        (face == Direction.UP ? point.z() : 1.0F - point.z()) * 16.0F));
            }
            result.add(new BakedQuad(data, first.getColorIndex(), face, sprite, first.hasShade()));
        }
        return List.copyOf(result);
    }

    static List<List<Point>> retile(List<List<Point>> polygons, boolean up) {
        List<Point> corners = new ArrayList<>();
        for (List<Point> polygon : polygons) {
            for (Point point : polygon) {
                if (!Float.isFinite(point.x()) || !Float.isFinite(point.z())
                        || point.x() < 0 || point.x() > 1 || point.z() < 0 || point.z() > 1) {
                    return List.of();
                }
                if (point.distanceSquared(new Point(0.5F, 0.5F)) < CORNER_EPSILON * CORNER_EPSILON) {
                    continue;
                }
                if (corners.stream().noneMatch(other ->
                        other.distanceSquared(point) < CORNER_EPSILON * CORNER_EPSILON)) {
                    corners.add(point);
                }
            }
        }
        // Leave third-party replacement geometry alone when it is not the authored 16-gon.
        if (corners.size() != 16) return List.of();
        corners.sort(Comparator.comparingDouble(point ->
                Math.atan2(point.z() - 0.5, point.x() - 0.5)));
        float radiusSquared = corners.get(0).distanceSquared(new Point(0.5F, 0.5F));
        for (int index = 0; index < 16; index++) {
            Point point = corners.get(index);
            Point next = corners.get((index + 1) % 16);
            double turn = Math.atan2(
                    (point.x() - 0.5) * (next.z() - 0.5) - (point.z() - 0.5) * (next.x() - 0.5),
                    (point.x() - 0.5) * (next.x() - 0.5) + (point.z() - 0.5) * (next.z() - 0.5));
            if (Math.abs(point.distanceSquared(new Point(0.5F, 0.5F)) - radiusSquared) > CORNER_EPSILON
                    || Math.abs(turn - Math.PI / 8) > 0.001) return List.of();
        }
        List<List<Point>> result = new ArrayList<>(8);
        Point centre = new Point(0.5F, 0.5F);
        for (int index = 0; index < 16; index += 2) {
            Point a = corners.get(index);
            Point b = corners.get((index + 1) % 16);
            Point c = corners.get((index + 2) % 16);
            result.add(up ? List.of(centre, c, b, a) : List.of(centre, a, b, c));
        }
        return List.copyOf(result);
    }

    record Point(float x, float z) {
        float distanceSquared(Point other) {
            float dx = x - other.x;
            float dz = z - other.z;
            return dx * dx + dz * dz;
        }
    }
}
