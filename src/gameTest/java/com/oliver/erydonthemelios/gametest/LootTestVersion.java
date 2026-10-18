package com.oliver.erydonthemelios.gametest;

import net.minecraft.server.MinecraftServer;
import java.util.Collection;

/** The only version-specific data-pack API details in the shared server test. */
final class LootTestVersion {
    static final String LOOT_FOLDER = "loot_table";
    static final int DATA_PACK_FORMAT = 48;
    static Collection<String> enabledPacks(MinecraftServer server) {
        return server.getDataPackManager().getEnabledIds();
    }
}
