package com.oliver.erydonthemelios;

import com.oliver.erydonthemelios.block.HorizontalSliceBlock;
import com.oliver.erydonthemelios.block.LayerBlock;
import com.oliver.erydonthemelios.block.LayerVerticalBlock;
import com.oliver.erydonthemelios.block.PostBlock;
import com.oliver.erydonthemelios.block.VerticalSliceBlock;
import com.oliver.erydonthemelios.block.CylinderBlock;
import com.oliver.erydonthemelios.block.CorniceBlock;
import com.oliver.erydonthemelios.item.ThemeliosBlockItem;
import com.oliver.erydonthemelios.migration.ThemeliosIdMigration;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModBlocks {
    private static final Map<String, Block> BLOCKS = registerBlocks();

    public static final Block NERIUM_BLOCK = BLOCKS.get("nerium_block");
    public static final Block GLACIUM_BLOCK = BLOCKS.get("glacium_block");

    private ModBlocks() {
    }

    public static void register() {
        ErydonThemelios.LOGGER.info("[{}] Registered {} blocks", ErydonThemelios.MOD_ID, BLOCKS.size());
    }

    public static List<Block> fullBlocks() {
        return blocksFor(ModBlockIds.FULL_BLOCKS);
    }

    public static List<Block> allBlocks() {
        return List.copyOf(BLOCKS.values());
    }

    public static List<Block> orderedBlocks() {
        return ModBlockOrdering.orderedBlocks(BLOCKS);
    }

    private static Map<String, Block> registerBlocks() {
        Map<String, Block> blocks = new LinkedHashMap<>();
        for (String name : ModBlockIds.FULL_BLOCKS) {
            blocks.put(name, registerBlock(name, createStoneBlock()));
        }
        for (String name : ModBlockIds.SLABS) {
            blocks.put(name, registerBlock(name, new SlabBlock(createStoneShapeSettings())));
        }
        for (String name : ModBlockIds.STAIRS) {
            blocks.put(name, registerBlock(name, new StairsBlock(Blocks.STONE.getDefaultState(), createStoneShapeSettings())));
        }
        for (String name : ModBlockIds.WALLS) {
            blocks.put(name, registerBlock(name, new WallBlock(createStoneShapeSettings())));
        }
        for (String name : ModBlockIds.POSTS) {
            blocks.put(name, registerBlock(name, new PostBlock(createStoneShapeSettings())));
        }
        for (String name : ModBlockIds.HORIZONTAL_SLICES) {
            blocks.put(name, registerBlock(name, new HorizontalSliceBlock(createStoneShapeSettings())));
        }
        for (String name : ModBlockIds.VERTICAL_SLICES) {
            blocks.put(name, registerBlock(name, new VerticalSliceBlock(createStoneShapeSettings())));
        }
        for (String name : ModBlockIds.LAYERS) {
            blocks.put(name, registerBlock(name, new LayerBlock(createStoneShapeSettings())));
        }
        for (String name : ModBlockIds.VERTICAL_LAYERS) {
            blocks.put(name, registerBlock(name, new LayerVerticalBlock(createStoneShapeSettings())));
        }
        for (String name : ModBlockIds.CYLINDER_SMALLS) {
            blocks.put(name, registerBlock(name, new CylinderBlock(createStoneShapeSettings(), 4.0, 12.0)));
        }
        for (String name : ModBlockIds.CYLINDER_LARGES) {
            blocks.put(name, registerBlock(name, new CylinderBlock(createStoneShapeSettings(), 0.0, 16.0)));
        }
        for (String name : ModBlockIds.CORNICE_SMALLS) {
            blocks.put(name, registerBlock(name,
                    new CorniceBlock(createStoneShapeSettings(), CorniceBlock.Size.SMALL)));
        }
        for (String name : ModBlockIds.CORNICE_LARGES) {
            blocks.put(name, registerBlock(name,
                    new CorniceBlock(createStoneShapeSettings(), CorniceBlock.Size.LARGE)));
        }
        return Collections.unmodifiableMap(blocks);
    }

    private static Block createStoneBlock() {
        return new Block(createStoneSettings());
    }

    private static AbstractBlock.Settings createStoneSettings() {
        return AbstractBlock.Settings.create()
                .mapColor(MapColor.IRON_GRAY)
                .strength(1.5f, 6.0f)
                .requiresTool();
    }

    private static AbstractBlock.Settings createStoneShapeSettings() {
        return createStoneSettings().nonOpaque();
    }

    private static Block registerBlock(String name, Block block) {
        String canonicalName = ThemeliosIdMigration.canonicalPath(name);
        registerBlockItem(canonicalName, block);
        return Registry.register(Registries.BLOCK, id(canonicalName), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, id(name), new ThemeliosBlockItem(block, new Item.Settings()));
    }

    private static Identifier id(String name) {
        return Identifier.of(ErydonThemelios.MOD_ID, name);
    }

    private static List<Block> blocksFor(List<String> names) {
        return names.stream().map(BLOCKS::get).toList();
    }
}
