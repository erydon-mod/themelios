package com.oliver.erydonthemelios.client.texturealias;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.resource.ResourcePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elects one installed ERYDON-family mod to install the shared alias wrapper.
 * Every family mod carries the resolver so it also remains fully standalone.
 */
public final class FamilyTextureAliasCoordinator {
    public static final String CAPABILITY_KEY = "erydon:texture_alias_resolver";
    public static final int CAPABILITY_VERSION = 1;

    private static final String SELF_MOD_ID = "themelios";
    private static final Logger LOGGER =
            LoggerFactory.getLogger("Themelios/TextureAliasCoordinator");
    private static final AtomicBoolean LEADER_LOGGED = new AtomicBoolean();

    private FamilyTextureAliasCoordinator() {
    }

    public static ResourcePack wrapFamilyNamespaces(ResourcePack pack) {
        return isLeader()
                ? TextureAliasResourcePack.wrapFamilyNamespaces(pack)
                : pack;
    }

    public static boolean isLeader() {
        String leader = FabricLoader.getInstance().getAllMods().stream()
                .map(ModContainer::getMetadata)
                .filter(FamilyTextureAliasCoordinator::hasResolverCapability)
                .map(ModMetadata::getId)
                .filter(FamilyTextureAliasCoordinator::isFamilyResolver)
                .min(Comparator
                        .comparingInt(FamilyTextureAliasCoordinator::priority)
                        .thenComparing(id -> id))
                .orElse(SELF_MOD_ID);
        if (LEADER_LOGGED.compareAndSet(false, true)) {
            LOGGER.info("Texture alias resolver leader: {}", leader);
        }
        return SELF_MOD_ID.equals(leader);
    }

    private static boolean hasResolverCapability(ModMetadata metadata) {
        CustomValue value = metadata.getCustomValue(CAPABILITY_KEY);
        return value != null
                && value.getType() == CustomValue.CvType.NUMBER
                && value.getAsNumber().intValue() >= CAPABILITY_VERSION;
    }

    private static boolean isFamilyResolver(String modId) {
        return priority(modId) < Integer.MAX_VALUE;
    }

    private static int priority(String modId) {
        return switch (modId) {
            case "erydon" -> 0;
            case "erydon_themelios", "themelios" -> 1;
            case "daedalon" -> 2;
            default -> Integer.MAX_VALUE;
        };
    }
}
