package com.oliver.erydonthemelios;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
    public static final ItemGroup ERYDON_THEMELIOS = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(ErydonThemelios.MOD_ID, "themelios"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.EMBLEM))
                    .displayName(Text.translatable("itemGroup.themelios"))
                    .entries((context, entries) -> ModBlocks.orderedBlocks().forEach(entries::add))
                    .build()
    );

    private ModItemGroups() {
    }

    public static void register() {
        ErydonThemelios.LOGGER.info("[{}] Creative tab registered", ErydonThemelios.MOD_ID);
    }
}
