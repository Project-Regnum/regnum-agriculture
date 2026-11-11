// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.config;

import com.google.gson.*;
import com.projectregnum.agriculture.season.Season;
import com.projectregnum.agriculture.season.SeasonCalendar;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Validated immutable server policy. A client's local file cannot replace this policy. */
public record ServerConfig(int seasonLengthDays, boolean enableSeasonalColors,
        boolean enableSeasonalWeather, boolean enableSeasonalCropGrowth,
        Map<String, Double> cropGrowthMultipliers, Map<String, Map<String, Double>> cropOverrides,
        Set<String> seasonalDimensions) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    public static final double MAX_MULTIPLIER = 16;

    public static ServerConfig defaults() {
        return new ServerConfig(24, true, true, true,
                Map.of("spring", 1.15, "summer", 1.25, "autumn", 0.85, "winter", 0.35),
                Map.of(), Set.of("minecraft:overworld"));
    }

    public static ServerConfig parse(String text, Consumer<String> warn) {
        JsonElement element = JsonParser.parseString(text);
        if (!element.isJsonObject()) throw new JsonParseException("Config must be an object");
        JsonObject json = element.getAsJsonObject();
        ServerConfig defaults = defaults();
        int length = defaults.seasonLengthDays();
        if (json.has("seasonLengthDays")) {
            try {
                JsonPrimitive value = json.getAsJsonPrimitive("seasonLengthDays");
                double number = value.getAsDouble();
                if (!value.isNumber() || number != Math.rint(number) || number < 1
                        || number > SeasonCalendar.MAX_LENGTH_DAYS) throw new IllegalArgumentException();
                length = (int) number;
            } catch (RuntimeException e) { warn.accept("Invalid seasonLengthDays; using 24"); }
        }
        Map<String, Double> multipliers = readMultipliers(json.get("cropGrowthMultipliers"), defaults.cropGrowthMultipliers(), warn);
        Map<String, Map<String, Double>> overrides = new HashMap<>();
        JsonElement rawOverrides = json.get("cropOverrides");
        if (rawOverrides != null) {
            if (rawOverrides.isJsonObject()) {
                rawOverrides.getAsJsonObject().entrySet().forEach(entry -> {
                    if (ID.matcher(entry.getKey()).matches() && entry.getValue().isJsonObject()) {
                        overrides.put(entry.getKey(), readMultipliers(entry.getValue(), multipliers, warn));
                    } else warn.accept("Invalid crop override: " + entry.getKey());
                });
            } else warn.accept("cropOverrides must be an object");
        }
        Set<String> dimensions = new HashSet<>(defaults.seasonalDimensions());
        if (json.has("seasonalDimensions")) {
            JsonElement raw = json.get("seasonalDimensions");
            if (raw.isJsonArray()) {
                dimensions.clear();
                for (JsonElement value : raw.getAsJsonArray()) {
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                            && ID.matcher(value.getAsString()).matches()) dimensions.add(value.getAsString());
                    else warn.accept("Invalid seasonal dimension entry");
                }
            } else warn.accept("seasonalDimensions must be an array");
        }
        return new ServerConfig(length, bool(json, "enableSeasonalColors", true, warn),
                bool(json, "enableSeasonalWeather", true, warn), bool(json, "enableSeasonalCropGrowth", true, warn),
                Map.copyOf(multipliers), Map.copyOf(overrides), Set.copyOf(dimensions));
    }

    private static Map<String, Double> readMultipliers(JsonElement value, Map<String, Double> fallback, Consumer<String> warn) {
        Map<String, Double> result = new HashMap<>(fallback);
        if (value == null) return Map.copyOf(result);
        if (!value.isJsonObject()) { warn.accept("Multipliers must be an object"); return Map.copyOf(result); }
        JsonObject object = value.getAsJsonObject();
        for (Season season : Season.values()) {
            if (!object.has(season.id())) continue;
            try {
                JsonPrimitive primitive = object.getAsJsonPrimitive(season.id());
                double multiplier = primitive.getAsDouble();
                if (!primitive.isNumber() || !Double.isFinite(multiplier) || multiplier < 0 || multiplier > MAX_MULTIPLIER)
                    throw new IllegalArgumentException();
                result.put(season.id(), multiplier);
            } catch (RuntimeException e) { warn.accept("Invalid " + season.id() + " multiplier; using default"); }
        }
        return Map.copyOf(result);
    }

    private static boolean bool(JsonObject json, String key, boolean fallback, Consumer<String> warn) {
        if (!json.has(key)) return fallback;
        JsonElement value = json.get(key);
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean();
        warn.accept("Invalid " + key + "; using default");
        return fallback;
    }

    public double cropMultiplier(String crop, Season season) {
        return cropOverrides.getOrDefault(crop, cropGrowthMultipliers).get(season.id());
    }
}
