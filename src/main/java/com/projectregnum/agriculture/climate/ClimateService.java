// SPDX-License-Identifier: MPL-2.0
// Temperature bands adapted from FabricSeasons.getSeasonWeather, revision e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c.
// Original authors: D4rkness_King and Fabric Seasons contributors. See THIRD_PARTY_NOTICES.md.
package com.projectregnum.agriculture.climate;

import com.projectregnum.agriculture.season.Season;
import com.projectregnum.agriculture.season.SeasonManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class ClimateService {
    private ClimateService() {}

    /** Keeps hot/dry biomes dry and permanently cold biomes cold. Does not mutate registry biomes. */
    public static float temperature(float original, Season season) {
        float autumn = original <= -0.51f ? -0.30f : original <= 0.15f ? -0.25f
                : original <= 0.49f ? -0.16f : -0.34f;
        float summer = original <= -0.51f ? 0.84f : original <= 0.49f ? 0.66f
                : original <= 0.79f ? 0.46f : 0.40f;
        float winter = original <= -0.51f ? -0.70f : original <= 0.15f ? -0.75f
                : original <= 0.49f ? -0.80f : original <= 0.79f ? -0.56f : -0.64f;
        return original + switch (season) {
            case SPRING -> 0;
            case SUMMER -> summer;
            case AUTUMN -> autumn;
            case WINTER -> winter;
        };
    }
    public static boolean enabled(ServerLevel level) {
        SeasonManager manager = SeasonManager.get(level.getServer());
        return manager.isSeasonal(level) && manager.config().enableSeasonalWeather();
    }
    public static boolean forceWinterSnow(Holder<Biome> biome) {
        // Retain upstream's explicit plains/sunflower plains/stony peaks exceptions.
        return biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS) || biome.is(Biomes.STONY_PEAKS);
    }
    public static float temperatureAt(Biome biome, BlockPos pos, Season season, boolean forceSnow) {
        float original = ((BiomeTemperatureAccess) (Object) biome).regnum$temperatureAt(pos);
        return forceSnow && season == Season.WINTER && original >= 0.15f ? 0.14f : temperature(original, season);
    }
    public static float temperature(ServerLevel level, Biome biome, BlockPos pos) {
        float original = ((BiomeTemperatureAccess) (Object) biome).regnum$temperatureAt(pos);
        return enabled(level) ? temperatureAt(biome, pos, SeasonManager.get(level.getServer()).getCurrentSeason(level),
                forceWinterSnow(level.getBiome(pos))) : original;
    }
}
