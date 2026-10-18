package com.oliver.erydonthemelios;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item EMBLEM = Registry.register(
            Registries.ITEM,
            Identifier.of(ErydonThemelios.MOD_ID, "emblem"),
            new Item(new Item.Settings())
    );

    private ModItems() {
    }

    public static void register() {
        ErydonThemelios.LOGGER.info("[{}] Items registered", ErydonThemelios.MOD_ID);
    }
}
