// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture;

import com.projectregnum.agriculture.agriculture.GrowthProbability;
import com.projectregnum.agriculture.config.*;
import com.projectregnum.agriculture.season.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FoundationTest {
    @Test void initialSeasonAndOneBasedDay() {
        var snapshot = SeasonCalendar.at(0, 0, 0, 24);
        assertEquals(Season.SPRING, snapshot.season());
        assertEquals(1, snapshot.day());
        assertEquals(0, snapshot.progress());
    }
    @Test void everyBoundaryAndYearWrap() {
        long length = SeasonCalendar.seasonTicks(24);
        for (int i = 0; i < 12; i++) {
            var before = SeasonCalendar.at((i + 1) * length - 1, 0, 0, 24);
            var after = SeasonCalendar.at((i + 1) * length, 0, 0, 24);
            assertEquals(Season.values()[i % 4], before.season());
            assertEquals(24, before.day());
            assertEquals(Season.values()[(i + 1) % 4], after.season());
            assertEquals(1, after.day());
            assertTrue(before.progress() > 0.999f);
            assertEquals(0, after.progress());
        }
    }
    @Test void configurableLengthAndNegativeTime() {
        assertEquals(Season.SUMMER, SeasonCalendar.at(48_000, 0, 0, 2).season());
        assertEquals(Season.WINTER, SeasonCalendar.at(-1, 0, 0, 24).season());
        assertEquals(12, SeasonCalendar.at(11 * 24_000, 0, 0, 24).day());
        assertThrows(IllegalArgumentException.class, () -> SeasonCalendar.seasonTicks(0));
        assertThrows(IllegalArgumentException.class, () -> SeasonCalendar.seasonTicks(Integer.MAX_VALUE));
    }
    @Test void arbitraryLongTimeDoesNotOverflow() {
        long year = 4 * SeasonCalendar.seasonTicks(24);
        long result = SeasonCalendar.cycleTick(Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, 24);
        var expected = java.math.BigInteger.valueOf(Long.MAX_VALUE).subtract(java.math.BigInteger.valueOf(Long.MIN_VALUE))
                .add(java.math.BigInteger.valueOf(Long.MAX_VALUE)).mod(java.math.BigInteger.valueOf(year)).longValueExact();
        assertEquals(expected, result);
    }
    @Test void lengthReloadPreservesSeasonAndFraction() {
        long old = SeasonCalendar.seasonTicks(24);
        long resized = SeasonCalendar.resize(2 * old + old / 2, 24, 7);
        var result = SeasonCalendar.fromCycleTick(resized, 7);
        assertEquals(Season.AUTUMN, result.season());
        assertEquals(0.5f, result.progress());
    }
    @Test void manualAnchorDoesNotNeedClockMutation() {
        long time = 123_456;
        long phase = 3 * SeasonCalendar.seasonTicks(24);
        assertEquals(Season.WINTER, SeasonCalendar.at(time, time, phase, 24).season());
        assertEquals(2, SeasonCalendar.at(time + 24_000, time, phase, 24).day());
    }
    @Test void missingConfigurationUsesDefaults() {
        assertEquals(ServerConfig.defaults(), ServerConfig.parse("{}", warning -> fail(warning)));
    }
    @Test void invalidFieldsFallbackIndividually() {
        List<String> warnings = new ArrayList<>();
        var config = ServerConfig.parse("""
                {"seasonLengthDays":0,"enableSeasonalColors":"false", "enableSeasonalWeather":false,
                 "cropGrowthMultipliers":{"spring":-2,"summer":999,"autumn":null,"winter":0}}
                """, warnings::add);
        assertEquals(24, config.seasonLengthDays());
        assertTrue(config.enableSeasonalColors());
        assertFalse(config.enableSeasonalWeather());
        assertEquals(1.15, config.cropMultiplier("minecraft:wheat", Season.SPRING));
        assertEquals(1.25, config.cropMultiplier("minecraft:wheat", Season.SUMMER));
        assertEquals(0.85, config.cropMultiplier("minecraft:wheat", Season.AUTUMN));
        assertEquals(0, config.cropMultiplier("minecraft:wheat", Season.WINTER));
        assertEquals(5, warnings.size());
    }
    @Test void invalidNumericFormsCannotCrashServer() {
        for (String value : List.of("null", "[]", "{}", "true", "\"NaN\"", "1e999", "-1", "17")) {
            var config = ServerConfig.parse("{\"cropGrowthMultipliers\":{\"winter\":" + value + "}}", warning -> {});
            assertEquals(0.35, config.cropMultiplier("minecraft:carrots", Season.WINTER));
        }
        var config = ServerConfig.parse("{\"seasonLengthDays\":1.5}", warning -> {});
        assertEquals(24, config.seasonLengthDays());
    }
    @Test void overridesAndDimensionAllowListAreImmutable() {
        var config = ServerConfig.parse("""
                {"cropOverrides":{"minecraft:wheat":{"winter":0.6},"Bad Id":{}},
                 "seasonalDimensions":["minecraft:overworld","mod:surface",null,"INVALID"]}
                """, warning -> {});
        assertEquals(0.6, config.cropMultiplier("minecraft:wheat", Season.WINTER));
        assertEquals(0.35, config.cropMultiplier("minecraft:potatoes", Season.WINTER));
        assertEquals(Set.of("minecraft:overworld", "mod:surface"), config.seasonalDimensions());
        assertThrows(UnsupportedOperationException.class, () -> config.cropOverrides().get("minecraft:wheat").put("winter", 2.0));
        assertFalse(config.seasonalDimensions().contains("minecraft:the_nether"));
    }
    @Test void malformedReloadRetainsPolicyAndFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("regnum_agriculture.json");
        var store = new ConfigStore(file, warning -> {});
        assertTrue(store.reload());
        assertTrue(Files.readString(file).contains("seasonLengthDays"));
        Files.writeString(file, "{\"seasonLengthDays\":7}");
        assertTrue(store.reload());
        Files.writeString(file, "{invalid");
        assertFalse(store.reload());
        assertEquals(7, store.current().seasonLengthDays());
        assertEquals("{invalid", Files.readString(file));
        var restart = new ConfigStore(file, warning -> {});
        assertFalse(restart.reload());
        assertEquals(24, restart.current().seasonLengthDays());
    }
    @Test void growthProbabilityPreservesVanillaAndWinter() {
        for (int bound = 1; bound <= 100; bound++) {
            assertEquals(1.0 / bound, GrowthProbability.modified(bound, 1));
            assertEquals(0.35 / bound, GrowthProbability.modified(bound, 0.35));
            assertEquals(Math.min(1, 1.25 / bound), GrowthProbability.modified(bound, 1.25));
            assertEquals(0, GrowthProbability.modified(bound, 0));
        }
        assertEquals(1, GrowthProbability.modified(3, 16));
        assertEquals(1.0 / 3, GrowthProbability.modified(3, Double.NaN));
    }
    @Test void regnumWorldKeepsSingleGrowthOwner() {
        assertTrue(GrowthProbability.ownsCropTicks(false));
        assertFalse(GrowthProbability.ownsCropTicks(true));
    }
}
