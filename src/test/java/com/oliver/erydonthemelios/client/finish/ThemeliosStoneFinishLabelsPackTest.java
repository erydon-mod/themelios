package com.oliver.erydonthemelios.client.finish;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class ThemeliosStoneFinishLabelsPackTest {
    @Test void entireFreshCatalogueAndPatternOverridesUseTheCapturedFinishInThreeLanguages() throws Exception {
        var languages = ThemeliosStoneFinishLabelsPack.readLanguages();
        Map<String, String> finishes = new LinkedHashMap<>();
        int catalogue = 0;
        for (String key : languages.get("en_us").keySet()) {
            if (!key.startsWith("block.themelios.")) continue;
            catalogue++;
            String path = key.substring("block.themelios.".length());
            if (List.of("aged", "ashlar", "hewn", "rusticated", "rock", "diaphanes").stream()
                    .noneMatch(token -> List.of(path.split("_")).contains(token))) finishes.put(path, "mirror");
        }
        assertEquals(2940, catalogue);
        assertTrue(finishes.size() > 1000);
        finishes.put("aganite_block", "honed");
        finishes.put("aganite_herringbone_bronze_block", "polished");
        finishes.put("calacattum_portorium_weave_bronze_block", "honed");
        var pack = ThemeliosStoneFinishLabelsPack.create(languages, finishes);
        for (String language : List.of("en_us", "de_de", "es_es")) {
            JsonObject labels = labels(pack, language);
            assertEquals(finishes.size(), labels.size());
            for (var entry : finishes.entrySet()) {
                String key = "block.themelios." + entry.getKey();
                String label = switch(entry.getValue()) {
                    case "mirror" -> "Ganosis";
                    case "honed" -> language.equals("en_us") ? "Honed" : language.equals("de_de") ? "Geschliffen" : "Apomazado";
                    default -> language.equals("en_us") ? "Polished" : language.equals("de_de") ? "Poliert" : "Pulido";
                };
                assertEquals(languages.get(language).get(key) + " (" + label + ")", labels.get(key).getAsString());
            }
            for (String path : List.of("aganite_aged_block", "aganite_ashlar_block", "aganite_hewn_block", "aganite_rock_block"))
                assertFalse(labels.has("block.themelios." + path), path);
        }
        assertEquals("Aganite Georgian Coping (Ganosis)", ThemeliosStoneFinishLabelsPack.finishedName(
                "Aganite Polished Georgian Coping", "aganite_coping_georgian", "Ganosis"));
        assertEquals("Aganite Bronze Polished Inlay (Ganosis)", ThemeliosStoneFinishLabelsPack.finishedName(
                "Aganite Bronze Polished Inlay", "aganite_trim_bronze_block", "Ganosis"));
    }

    @Test void absentOptionalApiAndDataReloadsKeepTheirOriginalPacksAndNoTexturesAreExposed() throws Exception {
        var languages = ThemeliosStoneFinishLabelsPack.readLanguages();
        var pack = ThemeliosStoneFinishLabelsPack.create(languages, Map.of("aganite_block", "mirror"));
        List<ResourcePack> original = List.of(pack);
        assertSame(original, ThemeliosStoneFinishLabelsPack.append(ResourceType.SERVER_DATA, original, pack));
        assertSame(original, ThemeliosStoneFinishLabelsPack.append(ResourceType.CLIENT_RESOURCES, original, null));
        assertSame(pack, ThemeliosStoneFinishLabelsPack.append(ResourceType.CLIENT_RESOURCES, List.of(), pack).get(0));
        assertNull(pack.open(ResourceType.CLIENT_RESOURCES, Identifier.of("themelios", "textures/block/aganite_block.png")));
        assertNull(pack.open(ResourceType.SERVER_DATA, Identifier.of("themelios", "lang/en_us.json")));
        var textures = new HashSet<Identifier>();
        pack.findResources(ResourceType.CLIENT_RESOURCES, "themelios", "textures", (id, input) -> textures.add(id));
        assertTrue(textures.isEmpty());
        var empty = ThemeliosStoneFinishLabelsPack.create(languages, Map.of());
        assertSame(original, ThemeliosStoneFinishLabelsPack.append(ResourceType.CLIENT_RESOURCES, original, empty));
    }

    private static JsonObject labels(ResourcePack pack, String language) throws Exception {
        try (var stream = pack.open(ResourceType.CLIENT_RESOURCES, Identifier.of("themelios", "lang/" + language + ".json")).get()) {
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
