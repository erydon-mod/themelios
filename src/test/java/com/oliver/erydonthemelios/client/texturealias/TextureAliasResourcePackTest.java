package com.oliver.erydonthemelios.client.texturealias;

import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.oliver.erydonthemelios.client.texturealias.TextureAliasTestSupport.ALIAS;
import static com.oliver.erydonthemelios.client.texturealias.TextureAliasTestSupport.NAMESPACE;
import static com.oliver.erydonthemelios.client.texturealias.TextureAliasTestSupport.bytes;
import static com.oliver.erydonthemelios.client.texturealias.TextureAliasTestSupport.singleAliasPack;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextureAliasResourcePackTest {
    private static final String DISABLE_PROPERTY =
            "erydon.texture_aliases.disable";

    @AfterEach
    void clearDisableProperty() {
        System.clearProperty(DISABLE_PROPERTY);
    }

    @Test
    void missingPhysicalTextureResolvesThroughCanonicalBlob() throws Exception {
        TextureAliasTestSupport.AliasFixture fixture =
                singleAliasPack("alias-pack", "canonical");
        ResourcePack wrapped =
                TextureAliasResourcePack.wrap(fixture.pack(), NAMESPACE);

        assertArrayEquals(
                bytes("canonical"),
                read(wrapped.open(ResourceType.CLIENT_RESOURCES, ALIAS))
        );
    }

    @Test
    void physicalTextureInTheSamePackWinsOverAlias() throws Exception {
        TextureAliasTestSupport.AliasFixture fixture =
                singleAliasPack("override-pack", "canonical");
        fixture.pack().put(ALIAS, "physical");
        ResourcePack wrapped =
                TextureAliasResourcePack.wrap(fixture.pack(), NAMESPACE);

        assertArrayEquals(
                bytes("physical"),
                read(wrapped.open(ResourceType.CLIENT_RESOURCES, ALIAS))
        );
    }

    @Test
    void aliasedTextureIsDiscoverableAtItsLogicalPath() throws Exception {
        TextureAliasTestSupport.AliasFixture fixture =
                singleAliasPack("discoverable-pack", "canonical");
        ResourcePack wrapped =
                TextureAliasResourcePack.wrap(fixture.pack(), NAMESPACE);
        Map<Identifier, InputSupplier<InputStream>> found =
                new LinkedHashMap<>();

        wrapped.findResources(
                ResourceType.CLIENT_RESOURCES,
                NAMESPACE,
                "textures/block",
                found::put
        );

        assertArrayEquals(bytes("canonical"), read(found.get(ALIAS)));
    }

    @Test
    void invalidBlobHashFailsAndClosesDelegate() {
        TextureAliasTestSupport.AliasFixture fixture =
                singleAliasPack("bad-hash", "canonical");
        fixture.pack().put(fixture.blob(), "tampered");

        assertThrows(
                IllegalStateException.class,
                () -> TextureAliasResourcePack.wrap(fixture.pack(), NAMESPACE)
        );
        assertEquals(1, fixture.pack().closeCount());
    }

    @Test
    void systemPropertyProvidesPhysicalFallback() {
        TextureAliasTestSupport.AliasFixture fixture =
                singleAliasPack("disabled-pack", "canonical");
        System.setProperty(DISABLE_PROPERTY, "true");

        assertSame(
                fixture.pack(),
                TextureAliasResourcePack.wrap(fixture.pack(), NAMESPACE)
        );
    }

    @Test
    void familyWrapperSkipsNotReadyVirtualPackOutsideFamilyNamespaces() {
        TextureAliasTestSupport.NullStreamPack pack =
                new TextureAliasTestSupport.NullStreamPack(
                        "PFM-Runtime-RP",
                        Set.of("pfm")
                );

        assertSame(pack, TextureAliasResourcePack.wrapFamilyNamespaces(pack));
        assertEquals(0, pack.openCount());
        pack.close();
        assertEquals(1, pack.closeCount());
    }

    @Test
    void nullManifestStreamIsTreatedAsMissing() {
        TextureAliasTestSupport.NullStreamPack pack =
                new TextureAliasTestSupport.NullStreamPack(
                        "not-ready",
                        Set.of(NAMESPACE)
                );

        assertSame(pack, TextureAliasResourcePack.wrap(pack, NAMESPACE));
        assertEquals(1, pack.openCount());
        pack.close();
        assertEquals(1, pack.closeCount());
    }

    private static byte[] read(
            InputSupplier<InputStream> supplier
    ) throws IOException {
        try (InputStream input = supplier.get()) {
            return input.readAllBytes();
        }
    }
}
