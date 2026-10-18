package com.oliver.erydonthemelios.client.model;

import com.oliver.erydonthemelios.ErydonThemelios;
import com.oliver.erydonthemelios.ThemeliosShapeFamilies;
import com.oliver.erydonthemelios.migration.ThemeliosIdMigration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

final class SynapheiaService {
    private static final AtomicLong NEXT_GENERATION = new AtomicLong();
    private static final Map<SpriteKey, List<Sprite>> SPRITES = new ConcurrentHashMap<>();
    private static volatile Snapshot current = Snapshot.empty();

    private SynapheiaService() {
    }

    static Snapshot publish(SynapheiaManifest.Prepared prepared) {
        long generation = NEXT_GENERATION.incrementAndGet();
        Map<Identifier, LinkedHashSet<SynapheiaManifest.Rule>> mutable = new LinkedHashMap<>();
        for (SynapheiaManifest.Rule rule : prepared.rules()) {
            for (Identifier block : rule.blocks()) {
                mutable.computeIfAbsent(block, ignored -> new LinkedHashSet<>()).add(rule);
                if (!ErydonThemelios.MOD_ID.equals(block.getNamespace())) {
                    continue;
                }
                Identifier canonical = new Identifier(block.getNamespace(),
                        ThemeliosIdMigration.canonicalPath(block.getPath()));
                if (!canonical.equals(block)) {
                    mutable.computeIfAbsent(canonical, ignored -> new LinkedHashSet<>()).add(rule);
                }
                // Extend the active pack's full-block repeat rule only to its registered shape siblings.
                if (rule.method() == SynapheiaManifest.Method.REPEAT) {
                    for (String shape : ThemeliosShapeFamilies.shapesForFullBlock(canonical.getPath())) {
                        Identifier sibling = new Identifier(ErydonThemelios.MOD_ID, shape);
                        mutable.computeIfAbsent(sibling, ignored -> new LinkedHashSet<>()).add(rule);
                    }
                }
            }
        }
        Map<Identifier, List<SynapheiaManifest.Rule>> byBlock = new LinkedHashMap<>();
        mutable.forEach((block, rules) -> byBlock.put(block, List.copyOf(rules)));

        Snapshot published = new Snapshot(generation, Map.copyOf(byBlock),
                prepared.repeatRuleCount(), prepared.overlayRuleCount(), prepared.sourcePacks());
        SPRITES.clear();
        SynapheiaRepeatBakedModel.clearCaches();
        current = published;

        SynapheiaMetrics.event("resource_reload_phase", SynapheiaMode.SYNAPHEIA, generation, fields(
                "phase", "ctm_rule_publish", "state", "end", "status", "PASS",
                "duration_ns", prepared.durationNanos(), "rule_count", prepared.rules().size(),
                "repeat_rule_count", prepared.repeatRuleCount(),
                "overlay_rule_count", prepared.overlayRuleCount(),
                "block_count", byBlock.size(), "source_packs", prepared.sourcePacks()
        ));
        ErydonThemelios.LOGGER.info("[{}] Synapheia loaded {} repeat and {} connected-overlay rules for {} blocks (generation {}).",
                ErydonThemelios.MOD_ID, prepared.repeatRuleCount(), prepared.overlayRuleCount(),
                byBlock.size(), generation);
        return published;
    }

    static Snapshot current() {
        return current;
    }

    static List<Sprite> sprites(Snapshot snapshot, SynapheiaManifest.Rule rule) {
        if (snapshot.generation() != current.generation() || !snapshot.active()) {
            return null;
        }
        SpriteKey key = new SpriteKey(snapshot.generation(), rule.id());
        return SPRITES.computeIfAbsent(key, ignored -> {
            var atlas = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
            List<Sprite> result = rule.tiles().stream().map(atlas).toList();
            for (int index = 0; index < result.size(); index++) {
                Identifier actual = result.get(index).getContents().getId();
                if (!rule.tiles().get(index).equals(actual)) {
                    throw new IllegalStateException("Synapheia sprite " + rule.tiles().get(index)
                            + " is missing from the block atlas for rule " + rule.resourceId() + ".");
                }
            }
            return result;
        });
    }

    private static Map<String, Object> fields(Object... entries) {
        Map<String, Object> fields = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            fields.put((String) entries[index], entries[index + 1]);
        }
        return fields;
    }

    record Snapshot(long generation,
                    Map<Identifier, List<SynapheiaManifest.Rule>> rulesByBlock,
                    int repeatRuleCount,
                    int overlayRuleCount,
                    String sourcePacks) {
        static Snapshot empty() {
            return new Snapshot(0L, Map.of(), 0, 0, "<initial>");
        }

        boolean active() {
            return !rulesByBlock.isEmpty();
        }

        List<SynapheiaManifest.Rule> rulesFor(Identifier blockId) {
            return rulesByBlock.getOrDefault(blockId, List.of());
        }

        SynapheiaManifest.Rule repeatRuleFor(Identifier blockId,
                                               Direction face,
                                               Identifier sourceSprite) {
            List<SynapheiaManifest.Rule> rules = rulesFor(blockId);
            for (SynapheiaManifest.Rule rule : rules) {
                if (rule.method() == SynapheiaManifest.Method.REPEAT
                        && rule.tiles().contains(sourceSprite)) {
                    return null;
                }
            }
            for (SynapheiaManifest.Rule rule : rules) {
                if (rule.method() == SynapheiaManifest.Method.REPEAT
                        && rule.matches(face, sourceSprite)) {
                    return rule;
                }
            }
            return null;
        }

        List<SynapheiaManifest.Rule> overlayRulesFor(Identifier blockId,
                                                      Direction face,
                                                      Identifier sourceSprite) {
            List<SynapheiaManifest.Rule> result = new ArrayList<>();
            for (SynapheiaManifest.Rule rule : rulesFor(blockId)) {
                if (rule.method() == SynapheiaManifest.Method.OVERLAY_CTM
                        && rule.matches(face, sourceSprite)) {
                    result.add(rule);
                }
            }
            return List.copyOf(result);
        }
    }

    private record SpriteKey(long generation, String ruleId) {
    }
}
