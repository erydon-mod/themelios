package com.oliver.erydonthemelios.gametest;

import net.minecraft.server.MinecraftServer;
import java.util.Collection;

/** The only version-specific data-pack API details in the shared server test. */
final class LootTestVersion {
    static final String LOOT_FOLDER = "loot_tables";
    static final int DATA_PACK_FORMAT = 15;
    static Collection<String> enabledPacks(MinecraftServer server) {
        return server.getDataPackManager().getEnabledNames();
    }
}
