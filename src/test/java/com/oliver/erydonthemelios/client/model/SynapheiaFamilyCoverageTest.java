package com.oliver.erydonthemelios.client.model;

import com.oliver.erydonthemelios.ThemeliosShapeFamilies;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SynapheiaFamilyCoverageTest {
    private static final List<String> SHAPE_SUFFIXES = List.of(
            "_slice_horizontal", "_slice_vertical", "_layer_vertical",
            "_cylinder_small", "_cylinder_large", "_cornice_small", "_cornice_large",
            "_slab", "_stairs", "_wall", "_post", "_layer");

    @Test
    void everyNativeShapeInheritsItsFullBlocksRepeatRules() throws Exception {
        SynapheiaService.Snapshot snapshot = publish(productionRules());
        Path blockstates = Path.of("src/main/resources/assets/themelios/blockstates");
        int checked = 0;
        try (var paths = Files.list(blockstates)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".json")).toList()) {
                String shape = path.getFileName().toString().replace(".json", "");
                String suffix = SHAPE_SUFFIXES.stream().filter(shape::endsWith).findFirst().orElse(null);
                if (suffix == null) {
                    continue;
                }
                String fullBlock = shape.substring(0, shape.length() - suffix.length()) + "_block";
                assertTrue(Files.exists(blockstates.resolve(fullBlock + ".json")), shape);
                List<SynapheiaManifest.Rule> fullBlockRules = snapshot.rulesFor(id("themelios:" + fullBlock))
                        .stream().filter(rule -> rule.method() == SynapheiaManifest.Method.REPEAT).toList();
                assertFalse(fullBlockRules.isEmpty(), fullBlock);
                assertTrue(snapshot.rulesFor(id("themelios:" + shape)).containsAll(fullBlockRules), shape);
                checked++;
            }
        }
        assertTrue(checked > 2000, "The test must cover the complete registered shape catalogue.");
    }

    @Test
    void oldPackFullBlockRuleKeepsItsTilesFaceFilterAndSpriteFilterOnShapes() {
        Identifier source = id("erydon:block/aganite_block");
        SynapheiaManifest.Rule pack = rule("pack_base", SynapheiaManifest.Method.REPEAT,
                Set.of(id("themelios:aganite_block")), Set.of(Direction.NORTH), Set.of(source), 30);
        SynapheiaManifest.Rule fallback = rule("fallback_base", SynapheiaManifest.Method.REPEAT,
                Set.of(id("themelios:aganite_block")), Set.of(Direction.values()), Set.of(source), 10);
        SynapheiaService.Snapshot snapshot = publish(List.of(pack, fallback));
        Identifier slab = id("themelios:aganite_slab");

        assertEquals(List.of(pack, fallback), snapshot.rulesFor(slab));
        assertSame(pack, snapshot.repeatRuleFor(slab, Direction.NORTH, source));
        assertSame(fallback, snapshot.repeatRuleFor(slab, Direction.UP, source));
        assertNull(snapshot.repeatRuleFor(slab, Direction.NORTH, id("erydon:block/aganite_block_aged")));
        assertNull(snapshot.repeatRuleFor(slab, Direction.NORTH, pack.tiles().get(0)));
        assertEquals(id("testpack:block/pack_base/0"), snapshot.rulesFor(slab).get(0).tiles().get(0));
        assertTrue(snapshot.rulesFor(id("themelios:aganite_aged_slab")).isEmpty());
        assertTrue(snapshot.rulesFor(id("themelios:aganite_ashlar_slab")).isEmpty());
        assertTrue(snapshot.overlayRulesFor(slab, Direction.NORTH, source).isEmpty());
    }

    @Test
    void agedAliasesKeepExplicitTargetsAndReachCanonicalShapes() {
        Identifier legacy = id("themelios:aganite_block_aged");
        Identifier canonical = id("themelios:aganite_aged_block");
        SynapheiaManifest.Rule aged = rule("aged_base", SynapheiaManifest.Method.REPEAT,
                Set.of(legacy), Set.of(Direction.values()), Set.of(id("erydon:block/aganite_block_aged")), 10);
        SynapheiaService.Snapshot snapshot = publish(List.of(aged));

        assertEquals(List.of(aged), snapshot.rulesFor(legacy));
        assertEquals(List.of(aged), snapshot.rulesFor(canonical));
        assertEquals(List.of(aged), snapshot.rulesFor(id("themelios:aganite_aged_slab")));
        assertEquals(List.of(aged), snapshot.rulesFor(id("themelios:aganite_aged_slice_vertical")));
        assertTrue(snapshot.rulesFor(id("themelios:aganite_slab")).isEmpty());

        SynapheiaManifest.Rule explicitLegacyShape = rule("legacy_shape", SynapheiaManifest.Method.REPEAT,
                Set.of(id("themelios:aganite_slab_aged")), Set.of(Direction.values()), Set.of(), 10);
        snapshot = publish(List.of(explicitLegacyShape));
        assertEquals(List.of(explicitLegacyShape), snapshot.rulesFor(id("themelios:aganite_slab_aged")));
        assertEquals(List.of(explicitLegacyShape), snapshot.rulesFor(id("themelios:aganite_aged_slab")));
        assertTrue(snapshot.rulesFor(canonical).isEmpty());
    }

    @Test
    void onlyRegisteredSameNamespaceFullBlockFamiliesExpand() {
        SynapheiaManifest.Rule unrelated = rule("unrelated", SynapheiaManifest.Method.REPEAT,
                Set.of(id("minecraft:aganite_block"), id("themelios:imaginary_block"),
                        id("themelios:aganite_trim_bronze_block")),
                Set.of(Direction.values()), Set.of(), 10);
        SynapheiaService.Snapshot snapshot = publish(List.of(unrelated));

        assertEquals(List.of(unrelated), snapshot.rulesFor(id("minecraft:aganite_block")));
        assertTrue(snapshot.rulesFor(id("minecraft:aganite_slab")).isEmpty());
        assertTrue(snapshot.rulesFor(id("themelios:aganite_slab")).isEmpty());
        assertTrue(snapshot.rulesFor(id("themelios:imaginary_slab")).isEmpty());
        assertTrue(snapshot.rulesFor(id("themelios:aganite_trim_bronze_slab")).isEmpty());
        assertTrue(ThemeliosShapeFamilies.shapesForFullBlock("imaginary_block").isEmpty());
        assertTrue(ThemeliosShapeFamilies.shapesForFullBlock("aganite_trim_bronze_block").isEmpty());

        SynapheiaManifest.Rule explicitShape = rule("slab_only", SynapheiaManifest.Method.REPEAT,
                Set.of(id("themelios:aganite_slab")), Set.of(Direction.values()), Set.of(), 10);
        snapshot = publish(List.of(explicitShape));
        assertEquals(List.of(explicitShape), snapshot.rulesFor(id("themelios:aganite_slab")));
        assertTrue(snapshot.rulesFor(id("themelios:aganite_stairs")).isEmpty());
        assertTrue(snapshot.rulesFor(id("themelios:aganite_block")).isEmpty());
    }

    @Test
    void connectedOverlaysRemainLimitedToTheirExplicitBlocks() {
        Identifier source = id("erydon:block/aganite_block");
        SynapheiaManifest.Rule overlay = rule("overlay", SynapheiaManifest.Method.OVERLAY_CTM,
                Set.of(id("themelios:aganite_block")), Set.of(Direction.values()), Set.of(source), 20);
        SynapheiaService.Snapshot snapshot = publish(List.of(overlay));

        assertEquals(List.of(overlay), snapshot.overlayRulesFor(id("themelios:aganite_block"),
                Direction.UP, source));
        assertTrue(snapshot.rulesFor(id("themelios:aganite_slab")).isEmpty());
        assertTrue(snapshot.rulesFor(id("themelios:aganite_cylinder_small")).isEmpty());
    }

    @Test
    void fullHeightLayersInheritTheirOwnMaterialBlock() throws Exception {
        Path models = Path.of("src/main/resources/assets/erydon/models/block/layer/layer");
        int checked = 0;
        try (var paths = Files.list(models)) {
            for (Path path : paths.filter(file -> file.toString().contains("height16")
                    && file.toString().endsWith(".json")).toList()) {
                var model = com.google.gson.JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if (!model.has("parent")) {
                    continue;
                }
                String parent = model.get("parent").getAsString();
                if (parent.startsWith("erydon:block/block/")) {
                    String block = path.getFileName().toString().replace(".json", "")
                            .replace("_layer_top_height16", "_block")
                            .replace("_layer_height16", "_block");
                    assertEquals("erydon:block/block/" + block, parent,
                            "A full-height layer must not borrow another material's full block: " + path);
                    checked++;
                }
            }
        }
        // Other full-height layers use authored geometry or a shared layer template.
        assertTrue(checked > 0, "Check the full-height layers that inherit a material block.");
    }

    private static List<SynapheiaManifest.Rule> productionRules() throws Exception {
        Path resources = Path.of("src/main/resources/assets/minecraft");
        List<SynapheiaManifest.Rule> rules = new ArrayList<>();
        try (var paths = Files.walk(resources.resolve("optifine/ctm"))) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".properties")).sorted().toList()) {
                Properties properties = new Properties();
                try (InputStream input = Files.newInputStream(path)) {
                    properties.load(input);
                }
                Set<Identifier> blocks = SynapheiaManifest.parseBlocks(properties.getProperty("matchBlocks"));
                if (!blocks.isEmpty()) {
                    String relative = resources.relativize(path).toString().replace('\\', '/');
                    rules.add(SynapheiaManifest.parseRule(id("minecraft:" + relative),
                            "production-test", properties, blocks));
                }
            }
        }
        return rules;
    }

    private static SynapheiaManifest.Rule rule(String name,
                                               SynapheiaManifest.Method method,
                                               Set<Identifier> blocks,
                                               Set<Direction> faces,
                                               Set<Identifier> matchTiles,
                                               int priority) {
        int tileCount = method == SynapheiaManifest.Method.REPEAT ? 36 : 47;
        List<Identifier> tiles = IntStream.range(0, tileCount)
                .mapToObj(index -> id("testpack:block/" + name + "/" + index)).toList();
        return new SynapheiaManifest.Rule(id("minecraft:optifine/ctm/" + name + ".properties"),
                "testpack", method, tiles, faces, blocks, matchTiles, true, priority,
                SynapheiaManifest.OverlayLayer.CUTOUT);
    }

    private static SynapheiaService.Snapshot publish(List<SynapheiaManifest.Rule> rules) {
        int repeat = (int) rules.stream().filter(rule -> rule.method() == SynapheiaManifest.Method.REPEAT).count();
        return SynapheiaService.publish(new SynapheiaManifest.Prepared(
                rules, "testpack", 1, repeat, rules.size() - repeat, 1L));
    }

    private static Identifier id(String value) {
        return Identifier.tryParse(value);
    }
}
