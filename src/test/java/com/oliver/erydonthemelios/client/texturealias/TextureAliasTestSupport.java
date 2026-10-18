package com.oliver.erydonthemelios.client.texturealias;

import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class TextureAliasTestSupport {
    static final String NAMESPACE = "erydon";
    static final String ALIAS_PATH = "textures/block/pilot_alias.png";
    static final Identifier ALIAS = Identifier.of(NAMESPACE, ALIAS_PATH);

    private TextureAliasTestSupport() {
    }

    static AliasFixture singleAliasPack(String name, String contents) {
        FakePack pack = new FakePack(name);
        return populateSingleAlias(pack, NAMESPACE, ALIAS_PATH, contents);
    }

    static AliasFixture populateSingleAlias(
            FakePack pack,
            String namespace,
            String aliasPath,
            String contents
    ) {
        Identifier alias = Identifier.of(namespace, aliasPath);
        byte[] canonicalBytes = bytes(contents);
        Identifier blob = blobId(namespace, canonicalBytes);
        pack.putBytes(blob, canonicalBytes);
        LinkedHashMap<Identifier, Identifier> aliases = new LinkedHashMap<>();
        aliases.put(alias, blob);
        addManifest(pack, namespace, aliases);
        return new AliasFixture(pack, alias, blob);
    }

    static void addManifest(
            FakePack pack,
            String namespace,
            Map<Identifier, Identifier> aliases
    ) {
        pack.put(
                Identifier.of(namespace, TextureAliasManifest.MANIFEST_PATH),
                manifest(namespace, aliases)
        );
    }

    static Identifier blobId(String namespace, byte[] contents) {
        return Identifier.of(
                namespace,
                "texture_blobs/" + sha256(contents) + ".png"
        );
    }

    static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(data)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String manifest(
            String namespace,
            Map<Identifier, Identifier> aliases
    ) {
        StringBuilder json = new StringBuilder(
                "{\"schema_version\":1,\"format\":\"erydon-texture-aliases\","
                        + "\"namespace\":\"" + namespace + "\",\"tier\":\"test\","
                        + "\"aliases\":["
        );
        boolean first = true;
        Set<Identifier> blobs = new LinkedHashSet<>();
        for (Map.Entry<Identifier, Identifier> entry : aliases.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            Identifier alias = entry.getKey();
            Identifier target = entry.getValue();
            String fileSha = target.getPath()
                    .substring("texture_blobs/".length(), target.getPath().length() - 4);
            String logicalPath = alias.getPath();
            if (logicalPath.startsWith("textures/")) {
                logicalPath = logicalPath.substring("textures/".length());
            }
            logicalPath = logicalPath.substring(0, logicalPath.length() - 4);
            json.append("{\"file_sha256\":\"").append(fileSha)
                    .append("\",\"logical_id\":\"")
                    .append(alias.getNamespace()).append(':').append(logicalPath)
                    .append("\",\"mcmeta_present\":false,\"mcmeta_sha256\":null,")
                    .append("\"path\":\"").append(alias.getPath())
                    .append("\",\"role\":\"albedo\",\"source\":\"test\",")
                    .append("\"target\":\"assets/").append(namespace).append('/')
                    .append(target.getPath()).append("\",\"tier\":\"test\"}");
            blobs.add(target);
        }
        json.append("],\"blobs\":[");
        first = true;
        for (Identifier blob : blobs) {
            if (!first) {
                json.append(',');
            }
            first = false;
            String fileSha = blob.getPath()
                    .substring("texture_blobs/".length(), blob.getPath().length() - 4);
            json.append("{\"file_sha256\":\"").append(fileSha)
                    .append("\",\"target\":\"assets/").append(namespace).append('/')
                    .append(blob.getPath()).append("\"}");
        }
        return json.append("]}").toString();
    }

    record AliasFixture(FakePack pack, Identifier alias, Identifier blob) {
    }

    static class FakePack implements ResourcePack {
        private final ResourcePackInfo info;
        private final Map<Identifier, byte[]> resources = new LinkedHashMap<>();
        private int closeCount;

        FakePack(String id) {
            this.info = new ResourcePackInfo(
                    id,
                    Text.literal(id),
                    ResourcePackSource.NONE,
                    Optional.empty()
            );
        }

        void put(Identifier id, String value) {
            putBytes(id, bytes(value));
        }

        void putBytes(Identifier id, byte[] value) {
            resources.put(id, value.clone());
        }

        int closeCount() {
            return closeCount;
        }

        @Override
        public InputSupplier<InputStream> openRoot(String... segments) {
            return null;
        }

        @Override
        public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
            byte[] value = type == ResourceType.CLIENT_RESOURCES
                    ? resources.get(id)
                    : null;
            return value == null
                    ? null
                    : () -> new ByteArrayInputStream(value);
        }

        @Override
        public void findResources(
                ResourceType type,
                String namespace,
                String prefix,
                ResultConsumer consumer
        ) {
            if (type != ResourceType.CLIENT_RESOURCES) {
                return;
            }
            resources.entrySet().stream()
                    .filter(entry -> entry.getKey().getNamespace().equals(namespace))
                    .filter(entry -> matchesPrefix(entry.getKey().getPath(), prefix))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> consumer.accept(
                            entry.getKey(),
                            () -> new ByteArrayInputStream(entry.getValue())
                    ));
        }

        @Override
        public Set<String> getNamespaces(ResourceType type) {
            if (type != ResourceType.CLIENT_RESOURCES) {
                return Set.of();
            }
            Set<String> namespaces = new LinkedHashSet<>();
            resources.keySet().forEach(id -> namespaces.add(id.getNamespace()));
            return namespaces;
        }

        @Override
        public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) {
            return null;
        }

        @Override
        public ResourcePackInfo getInfo() {
            return info;
        }

        @Override
        public void close() {
            closeCount++;
        }

        private static boolean matchesPrefix(String path, String prefix) {
            return prefix.isEmpty()
                    || path.equals(prefix)
                    || path.startsWith(prefix + "/");
        }
    }

    static final class NullStreamPack extends FakePack {
        private final Set<String> namespaces;
        private int openCount;

        NullStreamPack(String name, Set<String> namespaces) {
            super(name);
            this.namespaces = Set.copyOf(namespaces);
        }

        int openCount() {
            return openCount;
        }

        @Override
        public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
            if (type != ResourceType.CLIENT_RESOURCES) {
                return null;
            }
            openCount++;
            return () -> null;
        }

        @Override
        public Set<String> getNamespaces(ResourceType type) {
            return type == ResourceType.CLIENT_RESOURCES ? namespaces : Set.of();
        }
    }
}
