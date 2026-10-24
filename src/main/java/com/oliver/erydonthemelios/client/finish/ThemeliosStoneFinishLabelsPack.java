package com.oliver.erydonthemelios.client.finish;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oliver.erydonthemelios.ErydonThemelios;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Restart-bound language overlay. No texture, registry, world, tick or render work. */
public final class ThemeliosStoneFinishLabelsPack implements ResourcePack {
    private final Map<Identifier, byte[]> resources;
    private ThemeliosStoneFinishLabelsPack(Map<Identifier, byte[]> resources) { this.resources = Map.copyOf(resources); }

    public static List<ResourcePack> append(ResourceType type, List<ResourcePack> packs) {
        if (type != ResourceType.CLIENT_RESOURCES || !FabricLoader.getInstance().isModLoaded("erydon")) return packs;
        return append(type, packs, Cached.PACK);
    }

    static List<ResourcePack> append(ResourceType type, List<ResourcePack> packs, ThemeliosStoneFinishLabelsPack labels) {
        if (type != ResourceType.CLIENT_RESOURCES || labels == null || labels.resources.isEmpty()) return packs;
        List<ResourcePack> result = new ArrayList<>(packs); result.add(labels); return result;
    }

    static ThemeliosStoneFinishLabelsPack create(Map<String, Map<String, String>> languages, Map<String, String> finishes) {
        Map<Identifier, byte[]> resources = new LinkedHashMap<>();
        for (var language : languages.entrySet()) {
            var words = switch (language.getKey()) {
                case "en_us" -> Map.of("honed", "Honed", "polished", "Polished", "mirror", "Ganosis");
                case "de_de" -> Map.of("honed", "Geschliffen", "polished", "Poliert", "mirror", "Ganosis");
                case "es_es" -> Map.of("honed", "Apomazado", "polished", "Pulido", "mirror", "Ganosis");
                default -> throw new IllegalArgumentException("Unsupported finish language " + language.getKey());
            };
            JsonObject labels = new JsonObject();
            language.getValue().forEach((key, name) -> {
                if (!key.startsWith("block.themelios.") && !key.startsWith("item.themelios.")) return;
                String path = key.substring(key.indexOf("themelios.") + "themelios.".length());
                String finish = finishes.get(path);
                if (finish != null && words.containsKey(finish)) labels.addProperty(key, finishedName(name, path, words.get(finish)));
            });
            if (labels.size() > 0) resources.put(new Identifier("themelios", "lang/" + language.getKey() + ".json"),
                    labels.toString().getBytes(StandardCharsets.UTF_8));
        }
        return new ThemeliosStoneFinishLabelsPack(resources);
    }

    static String finishedName(String name, String path, String label) {
        String material = path.substring(0, path.indexOf('_'));
        String prefix = Character.toUpperCase(material.charAt(0)) + material.substring(1) + " ";
        if (name.startsWith(prefix)) {
            String rest = name.substring(prefix.length());
            for (String old : List.of("Honed", "Polished", "Mirror", "Ganosis", "Geschliffen", "Poliert",
                    "Spiegelglanz", "Apomazado", "Pulido", "Espejo")) {
                if (rest.startsWith(old + " ")) { name = prefix + rest.substring(old.length() + 1); break; }
            }
        }
        return name + " (" + label + ")";
    }

    static Map<String, Map<String, String>> readLanguages() throws IOException {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (String language : List.of("en_us", "de_de", "es_es")) {
            String path = "/assets/themelios/lang/" + language + ".json";
            try (InputStream input = ThemeliosStoneFinishLabelsPack.class.getResourceAsStream(path)) {
                if (input == null) throw new IOException("Missing bundled language " + path);
                var json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                Map<String, String> names = new LinkedHashMap<>();
                json.entrySet().forEach(entry -> names.put(entry.getKey(), entry.getValue().getAsString()));
                result.put(language, Map.copyOf(names));
            }
        }
        return Map.copyOf(result);
    }

    private static ThemeliosStoneFinishLabelsPack load() {
        try {
            var languages = readLanguages();
            var paths = languages.get("en_us").keySet().stream().filter(key -> key.startsWith("block.themelios.")
                    || key.startsWith("item.themelios.")).map(key -> key.substring(key.indexOf("themelios.") + "themelios.".length())).distinct().toList();
            var finishes = ErydonStoneFinishBridge.read(ThemeliosStoneFinishLabelsPack.class.getClassLoader(), paths);
            ErydonThemelios.LOGGER.info("[themelios] Stone finish names follow ERYDON's active preferences ({} items; restart required)", finishes.size());
            return create(languages, finishes);
        } catch (ReflectiveOperationException | IOException | LinkageError | RuntimeException failure) {
            ErydonThemelios.LOGGER.warn("[themelios] ERYDON finish names unavailable; original names retained", failure);
            return null;
        }
    }

    @Override public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        byte[] bytes = type == ResourceType.CLIENT_RESOURCES ? resources.get(id) : null;
        return bytes == null ? null : () -> new ByteArrayInputStream(bytes);
    }
    @Override public InputSupplier<InputStream> openRoot(String... segments) { return null; }
    @Override public Set<String> getNamespaces(ResourceType type) {
        return type == ResourceType.CLIENT_RESOURCES && !resources.isEmpty() ? Set.of("themelios") : Set.of();
    }
    @Override public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        if (type != ResourceType.CLIENT_RESOURCES) return;
        resources.forEach((id, bytes) -> {
            if (id.getNamespace().equals(namespace) && (prefix.isEmpty() || id.getPath().startsWith(prefix + "/")))
                consumer.accept(id, () -> new ByteArrayInputStream(bytes));
        });
    }
    @Override public <T> T parseMetadata(ResourceMetadataReader<T> reader) { return null; }
    @Override public String getName() { return "themelios:stone_finish_labels"; }
    @Override public void close() { }
    private static final class Cached { private static final ThemeliosStoneFinishLabelsPack PACK = load(); }
}
