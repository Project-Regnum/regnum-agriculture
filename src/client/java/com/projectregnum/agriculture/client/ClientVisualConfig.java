// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.client;

import com.google.gson.*;
import com.projectregnum.agriculture.RegnumAgriculture;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public record ClientVisualConfig(boolean enableSeasonalColors, boolean smoothTransitions) {
    public static ClientVisualConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("regnum_agriculture-client.json");
        try {
            if (!Files.exists(path)) Files.writeString(path, new GsonBuilder().setPrettyPrinting().create()
                    .toJson(new ClientVisualConfig(true, true)));
            JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            return new ClientVisualConfig(read(json, "enableSeasonalColors"), read(json, "smoothTransitions"));
        } catch (Exception e) {
            RegnumAgriculture.LOGGER.warn("Cannot read client visual config; using defaults: {}", e.getMessage());
            return new ClientVisualConfig(true, true);
        }
    }
    private static boolean read(JsonObject json, String name) {
        JsonElement value = json.get(name);
        return value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean() || value.getAsBoolean();
    }
}
