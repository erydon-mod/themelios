package com.oliver.erydonthemelios.gametest;

import com.oliver.erydonthemelios.block.CorniceBlock;
import com.oliver.erydonthemelios.migration.ThemeliosIdMigration;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Runs against the production registries and normally loaded vanilla loot manager. */
public final class BlockLootGameTests {
    private static final String PACK_FOLDER = "themelios-loot-validation-override";
    private static final String PACK_ID = "file/" + PACK_FOLDER;

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 1200)
    public void nativeQuantityAliasesWaterAndHigherPriorityDataPacks(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        List<String> original = new ArrayList<>(LootTestVersion.enabledPacks(server));
        // A previous failed test may have left its isolated-world fixture enabled.
        boolean wasEnabled = original.remove(PACK_ID);
        CompletableFuture<Void> initial = wasEnabled
                ? server.reloadResources(original) : CompletableFuture.completedFuture(null);
        CompletableFuture<?>[] pending = {initial};
        int[] phase = {0};
        Results results = new Results(context);
        context.runAtEveryTick(() -> {
            if (phase[0] == 3 || !pending[0].isDone()) return;
            try {
                // isDone above prevents blocking the server thread during either reload.
                pending[0].join();
                if (phase[0] == 0) {
                    checkNativeCatalogue(context, results);
                    results.assertSuccess();
                    writeOverridePack(server);
                    server.getDataPackManager().scanPacks();
                    List<String> enabled = new ArrayList<>(original);
                    enabled.add(PACK_ID); // Last enabled pack has the highest data priority.
                    phase[0] = 1;
                    pending[0] = server.reloadResources(enabled);
                } else if (phase[0] == 1) {
                    results.check("data_pack_nonempty_override", state("aganite_layer").with(
                            (IntProperty) state("aganite_layer").getBlock().getStateManager().getProperty("layers"), 8),
                            Items.DIAMOND, 3);
                    results.check("data_pack_empty_override", state("aganite_slab").with(SlabBlock.TYPE, SlabType.DOUBLE),
                            Items.AIR, 0);
                    phase[0] = 2;
                    pending[0] = server.reloadResources(original);
                } else {
                    BlockState layer = state("aganite_layer");
                    results.check("native_after_override_removed", layer.with(
                            (IntProperty) layer.getBlock().getStateManager().getProperty("layers"), 8), layer.getBlock().asItem(), 8);
                    results.check("native_slab_after_override_removed", state("aganite_slab").with(SlabBlock.TYPE, SlabType.DOUBLE),
                            state("aganite_slab").getBlock().asItem(), 2);
                    results.report();
                    results.assertSuccess();
                    phase[0] = 3;
                    context.complete();
                }
            } catch (IOException | RuntimeException exception) {
                phase[0] = 3;
                context.assertTrue(false, "Loot data-pack test failed: " + exception);
            }
        });
    }

    private static void checkNativeCatalogue(TestContext context, Results results) {
        results.check("vanilla_positive_control", Blocks.DIRT.getDefaultState(), Items.DIRT, 1);
        List<Block> blocks = Registries.BLOCK.stream()
                .filter(block -> Registries.BLOCK.getId(block).getNamespace().equals("themelios")).toList();
        context.assertTrue(blocks.size() == 2940, "Exercise all 2,940 canonical Themelios blocks");
        int stackables = 0, slabs = 0, cornices = 0;
        HashSet<Class<?>> orientationSamples = new HashSet<>();
        for (Block block : blocks) {
            BlockState state = block.getDefaultState();
            context.assertTrue(state.isToolRequired()
                            && Items.DIAMOND_PICKAXE.getDefaultStack().isSuitableFor(state)
                            && !ItemStack.EMPTY.isSuitableFor(state),
                    "Production harvest settings must require a suitable pickaxe: " + Registries.BLOCK.getId(block));
            results.check("canonical_default", state, block.asItem(), 1);
            if (state.contains(Properties.WATERLOGGED)) {
                results.check("waterlogged_default", state.with(Properties.WATERLOGGED, true), block.asItem(), 1);
            }
            if (block.getStateManager().getProperty("layers") instanceof IntProperty layers) {
                stackables++;
                for (int quantity = 1; quantity <= 8; quantity++) for (boolean water : new boolean[]{false, true}) {
                    results.check("all_stack_quantities_and_water", state.with(layers, quantity)
                            .with(Properties.WATERLOGGED, water), block.asItem(), quantity);
                }
                // Check every authored facing/top/post-axis state once per stackable family.
                if (orientationSamples.add(block.getClass())) for (BlockState oriented : block.getStateManager().getStates()) {
                    results.check("stack_orientation_matrix", oriented, block.asItem(), oriented.get(layers));
                }
            } else if (block instanceof SlabBlock) {
                slabs++;
                for (BlockState slab : block.getStateManager().getStates()) {
                    results.check("all_slab_states", slab, block.asItem(), slab.get(SlabBlock.TYPE) == SlabType.DOUBLE ? 2 : 1);
                }
            } else if (block instanceof CorniceBlock) {
                cornices++;
                for (BlockState cornice : block.getStateManager().getStates()) {
                    results.check("all_cornice_states", cornice, block.asItem(), cornice.get(CorniceBlock.SHAPE).isStraight() ? 1 : 2);
                }
            }
        }
        context.assertTrue(stackables == 1200 && orientationSamples.size() == 5 && slabs == 240 && cornices == 108,
                "Cover all five 240-member stackable families, all slabs, and both 54-member cornice sizes");
        for (var alias : ThemeliosIdMigration.aliases()) {
            Block canonical = Registries.BLOCK.get(alias.canonicalId());
            context.assertTrue(Registries.BLOCK.get(alias.oldId()) == canonical
                            && Registries.ITEM.get(alias.oldId()) == canonical.asItem(),
                    "Published block/item alias changed identity: " + alias.oldId());
            results.check("published_alias_canonical_loot", canonical.getDefaultState(), canonical.asItem(), 1);
        }
        results.report();
    }

    private static void writeOverridePack(MinecraftServer server) throws IOException {
        Path pack = server.getSavePath(WorldSavePath.DATAPACKS).resolve(PACK_FOLDER);
        Path tables = pack.resolve("data/themelios/" + LootTestVersion.LOOT_FOLDER + "/blocks");
        Files.createDirectories(tables);
        Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":"
                + LootTestVersion.DATA_PACK_FORMAT + ",\"description\":\"Themelios isolated loot regression fixture\"}}\n",
                StandardCharsets.UTF_8);
        // Override a full eight-layer stack without multiplying the pack author's explicit count.
        Files.writeString(tables.resolve("aganite_layer.json"), """
                {"type":"minecraft:block","pools":[{"rolls":1,"entries":[
                  {"type":"minecraft:item","name":"minecraft:diamond","functions":[
                    {"function":"minecraft:set_count","count":3}]}]}]}
                """, StandardCharsets.UTF_8);
        // A deliberate empty override must stay empty, rather than activating a fallback.
        Files.writeString(tables.resolve("aganite_slab.json"), "{\"type\":\"minecraft:block\",\"pools\":[]}\n",
                StandardCharsets.UTF_8);
    }

    private static BlockState state(String path) {
        Identifier id = Identifier.tryParse("themelios:" + path);
        Block block = Registries.BLOCK.get(id);
        if (block == Blocks.AIR) throw new IllegalStateException("Missing production block " + id);
        return block.getDefaultState();
    }

    private static final class Results {
        private final TestContext context;
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private final List<String> failures = new ArrayList<>();
        private int failureCount;

        private Results(TestContext context) { this.context = context; }

        private void check(String category, BlockState state, Item expected, int quantity) {
            ServerWorld world = context.getWorld();
            BlockPos pos = context.getAbsolutePos(new BlockPos(2, 2, 2));
            // The static loot API evaluates the supplied state/tool, without evaluating a
            // player's harvest gate. Catalogue tool suitability is asserted separately above.
            List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, null, null,
                    Items.DIAMOND_PICKAXE.getDefaultStack());
            int own = drops.stream().filter(stack -> stack.isOf(expected)).mapToInt(ItemStack::getCount).sum();
            int total = drops.stream().mapToInt(ItemStack::getCount).sum();
            counts.merge(category, 1, Integer::sum);
            if (own != quantity || total != quantity) {
                failureCount++;
                if (failures.size() < 12) failures.add(category + " " + state + " expected="
                        + quantity + "x" + Registries.ITEM.getId(expected) + " drops=" + drops);
            }
        }

        private void report() {
            System.out.println("THEMELIOS_LOOT_GAME_TEST cases=" + counts + " failures=" + failureCount);
        }

        private void assertSuccess() {
            context.assertTrue(failureCount == 0, "Loot failures=" + failureCount + ": " + String.join("; ", failures));
        }
    }
}
