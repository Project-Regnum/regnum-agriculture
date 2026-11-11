// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.agriculture;

import com.projectregnum.agriculture.season.SeasonManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CropGrowthService {
    private static final boolean OWNS_TICKS = GrowthProbability.ownsCropTicks(
            FabricLoader.getInstance().isModLoaded("regnum-world"));
    private CropGrowthService() {}
    public static boolean ownsCropTicks() { return OWNS_TICKS; }

    /** Seasonal policy remains queryable when Regnum World owns actual growth. */
    public static float getCropGrowthMultiplier(ServerLevel level, BlockPos position, BlockState crop) {
        SeasonManager manager = SeasonManager.get(level.getServer());
        if (!manager.isSeasonal(level) || !manager.config().enableSeasonalCropGrowth() || !supported(crop)) return 1;
        String id = BuiltInRegistries.BLOCK.getKey(crop.getBlock()).toString();
        return (float) manager.config().cropMultiplier(id, manager.getCurrentSeason(level));
    }

    public static boolean supported(BlockState state) {
        return state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES) || state.is(Blocks.BEETROOTS);
    }
}
