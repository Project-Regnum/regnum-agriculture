// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.mixin;

import com.projectregnum.agriculture.climate.ClimateService;
import com.projectregnum.agriculture.climate.BiomeTemperatureAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Biome.class)
public abstract class BiomeSnowMixin implements BiomeTemperatureAccess {
    @Shadow private float getTemperature(BlockPos pos) { throw new AssertionError(); }

    @Override
    @org.spongepowered.asm.mixin.Unique
    public float regnum$temperatureAt(BlockPos pos) { return getTemperature(pos); }

    @Redirect(method = "shouldSnow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;warmEnoughToRain(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean regnum$snowTemperature(Biome biome, BlockPos pos, LevelReader reader, BlockPos snowPos) {
        if (reader instanceof ServerLevel level && biome.hasPrecipitation() && ClimateService.enabled(level)) {
            // Keep vanilla frozen biomes unchanged; seasonal thaw applies only to our tracked snow.
            if (!biome.warmEnoughToRain(pos)) return false;
            return ClimateService.temperature(level, biome, pos) >= 0.15f;
        }
        return biome.warmEnoughToRain(pos);
    }
}
