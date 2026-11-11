// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.mixin;

import com.projectregnum.agriculture.climate.ClimateService;
import com.projectregnum.agriculture.climate.SeasonalSnowState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerWeatherMixin {
    @Redirect(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean regnum$trackSnow(ServerLevel level, BlockPos pos, BlockState next) {
        boolean seasonal = next.is(Blocks.SNOW) && level.getBlockState(pos).isAir()
                && ClimateService.enabled(level) && level.getBiome(pos).value().warmEnoughToRain(pos);
        boolean changed = level.setBlockAndUpdate(pos, next);
        if (changed && seasonal) SeasonalSnowState.get(level).add(pos);
        return changed;
    }

    @Redirect(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation regnum$precipitation(Biome biome, BlockPos pos) {
        ServerLevel level = (ServerLevel) (Object) this;
        Biome.Precipitation original = biome.getPrecipitationAt(pos);
        return original == Biome.Precipitation.RAIN && ClimateService.enabled(level)
                && ClimateService.temperature(level, biome, pos) < 0.15f ? Biome.Precipitation.SNOW : original;
    }

    @Inject(method = "tickPrecipitation", at = @At("HEAD"))
    private void regnum$thawSnow(BlockPos column, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
        regnum$thaw(level, top);
        regnum$thaw(level, top.below());
    }

    @org.spongepowered.asm.mixin.Unique
    private static void regnum$thaw(ServerLevel level, BlockPos pos) {
        SeasonalSnowState snow = SeasonalSnowState.get(level);
        if (!snow.contains(pos)) return;
        if (!level.getBlockState(pos).is(Blocks.SNOW)) { snow.remove(pos); return; }
        Biome biome = level.getBiome(pos).value();
        // Disabling weather/dimension effects also allows previously generated snow to thaw.
        if (!ClimateService.enabled(level) || ClimateService.temperature(level, biome, pos) >= 0.15f) {
            level.removeBlock(pos, false);
            snow.remove(pos);
        }
    }
}
