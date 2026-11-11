// SPDX-License-Identifier: MPL-2.0
// Adapted from Fabric Seasons mixin/BiomeMixin.java, revision e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c.
// Original authors: D4rkness_King and Fabric Seasons contributors. See THIRD_PARTY_NOTICES.md.
package com.projectregnum.agriculture.client.mixin;

import com.projectregnum.agriculture.client.*;
import com.projectregnum.agriculture.climate.ClimateService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class BiomeVisualMixin {
    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private void regnum$grass(double x, double z, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SeasonalColors.tint(cir.getReturnValue(), false));
    }
    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private void regnum$foliage(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(SeasonalColors.tint(cir.getReturnValue(), true));
    }
    @Inject(method = "getPrecipitationAt", at = @At("RETURN"), cancellable = true)
    private void regnum$precipitation(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        var client = net.minecraft.client.Minecraft.getInstance();
        // This mixin is also present in an integrated server process; render policy is client-thread only.
        if (client.isSameThread() && ClientSeasonState.weather() && cir.getReturnValue() == Biome.Precipitation.RAIN
                && ClimateService.temperatureAt((Biome) (Object) this, pos, ClientSeasonState.snapshot().season(),
                        ClimateService.forceWinterSnow(client.level.getBiome(pos))) < 0.15f) {
            cir.setReturnValue(Biome.Precipitation.SNOW);
        }
    }
}
