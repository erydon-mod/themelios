package com.oliver.erydonthemelios.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CorniceGeometryTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets");
    private static final double EPSILON = 1.0E-9;

    @Test
    void largeModelMatchesInteractionBoxesAndKeepsItsHeightsAndDepths() throws Exception {
        var field = CorniceBlock.Size.class.getDeclaredField("boxes");
        field.setAccessible(true);
        double[][] interaction = (double[][]) field.get(CorniceBlock.Size.LARGE);
        List<double[]> model = modelBoxes();
        assertEquals(3, model.size());
        double[][] heightAndDepth = {{10, 14, 6}, {14, 16, 8}, {0, 10, 4}};
        for (int tier = 0; tier < model.size(); tier++) {
            double[] box = model.get(tier);
            assertArrayEquals(box, interaction[tier], EPSILON, "Model/interaction tier " + tier);
            assertArrayEquals(heightAndDepth[tier],
                    new double[]{box[1], box[4], box[5]}, EPSILON);
            assertEquals(0, box[2], EPSILON);
            assertEquals(-box[5], box[0], EPSILON, "Left reach equals depth");
            assertEquals(16 + box[5], box[3], EPSILON, "Right reach equals depth");
        }
    }

    @Test
    void adjacentPerpendicularRunsFillBothCornerEndsAtEveryTierAndFacing() throws Exception {
        for (double[] box : modelBoxes()) {
            Rect north = footprint(box);
            double depth = box[5];
            // North run in the west cell, west run in the north cell; the corner cell is empty.
            Rect first = north.translate(-16, 0);
            Rect second = north.rotate(3);
            Rect corner = new Rect(0, 0, depth, depth);
            for (int facing = 0; facing < 4; facing++) {
                for (boolean otherEnd : new boolean[]{false, true}) {
                    Rect target = corner.orient(facing, otherEnd);
                    assertEquals(depth * depth,
                            unionArea(first.orient(facing, otherEnd),
                                    second.orient(facing, otherEnd), target),
                            EPSILON, "Unfilled corner at depth " + depth + ", facing " + facing);
                }
            }
            // A two-unit-short overhang reproduces the original 2 x 2 gap.
            Rect shortRun = new Rect(2 - depth, 0, 14 + depth, depth);
            assertEquals(4, depth * depth - unionArea(
                    shortRun.translate(-16, 0), shortRun.rotate(3), corner), EPSILON);
        }
    }

    @Test
    void adjacentStraightRunsRemainContinuousInEveryFacing() throws Exception {
        for (double[] box : modelBoxes()) {
            Rect first = footprint(box);
            Rect second = first.translate(16, 0);
            Rect target = new Rect(0, 0, 32, box[5]);
            for (int facing = 0; facing < 4; facing++) {
                assertEquals(32 * box[5], unionArea(first.rotate(facing),
                        second.rotate(facing), target.rotate(facing)), EPSILON);
            }
        }
    }

    @Test
    void allNormalAndAgedLargeCornicesKeepTheSharedParentAndUvLockedFacings() throws Exception {
        int checked = 0;
        try (var paths = Files.list(ASSETS.resolve("themelios/blockstates"))) {
            for (Path path : paths.filter(file -> file.toString().endsWith("_cornice_large.json")).toList()) {
                JsonObject blockstate = read(path);
                Set<Integer> rotations = new HashSet<>();
                for (var part : blockstate.getAsJsonArray("multipart")) {
                    JsonObject apply = part.getAsJsonObject().getAsJsonObject("apply");
                    assertTrue(apply.get("uvlock").getAsBoolean(), path.toString());
                    rotations.add(apply.has("y") ? apply.get("y").getAsInt() : 0);
                    String[] model = apply.get("model").getAsString().split(":", 2);
                    JsonObject child = read(ASSETS.resolve(model[0] + "/models/" + model[1] + ".json"));
                    assertEquals("erydon:block/cornice/cornice_large",
                            child.get("parent").getAsString(), path.toString());
                }
                assertEquals(Set.of(0, 90, 180, 270), rotations, path.toString());
                checked++;
            }
        }
        assertEquals(54, checked);
    }

    private static JsonObject read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static List<double[]> modelBoxes() throws Exception {
        List<double[]> boxes = new ArrayList<>();
        for (var element : read(ASSETS.resolve("erydon/models/block/cornice/cornice_large.json"))
                .getAsJsonArray("elements")) {
            JsonObject part = element.getAsJsonObject();
            double[] box = new double[6];
            for (int axis = 0; axis < 3; axis++) {
                box[axis] = part.getAsJsonArray("from").get(axis).getAsDouble();
                box[axis + 3] = part.getAsJsonArray("to").get(axis).getAsDouble();
            }
            boxes.add(box);
        }
        return boxes;
    }

    private static Rect footprint(double[] box) {
        return new Rect(box[0], box[2], box[3], box[5]);
    }

    private static double unionArea(Rect first, Rect second, Rect target) {
        Rect a = first.intersect(target);
        Rect b = second.intersect(target);
        return a.area() + b.area() - a.intersect(b).area();
    }

    private record Rect(double minX, double minZ, double maxX, double maxZ) {
        double area() {
            return Math.max(0, maxX - minX) * Math.max(0, maxZ - minZ);
        }

        Rect intersect(Rect other) {
            return new Rect(Math.max(minX, other.minX), Math.max(minZ, other.minZ),
                    Math.min(maxX, other.maxX), Math.min(maxZ, other.maxZ));
        }

        Rect translate(double x, double z) {
            return new Rect(minX + x, minZ + z, maxX + x, maxZ + z);
        }

        Rect rotate(int quarterTurns) {
            Rect result = this;
            for (int turn = 0; turn < quarterTurns; turn++) {
                result = new Rect(-result.maxZ, result.minX, -result.minZ, result.maxX);
            }
            return result;
        }

        Rect orient(int quarterTurns, boolean otherEnd) {
            Rect reflected = otherEnd ? new Rect(-maxX, minZ, -minX, maxZ) : this;
            return reflected.rotate(quarterTurns);
        }
    }
}
