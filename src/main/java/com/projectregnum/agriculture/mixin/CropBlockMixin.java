// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.mixin;

import com.projectregnum.agriculture.agriculture.CropGrowthService;
import com.projectregnum.agriculture.agriculture.GrowthProbability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
    // The soil/light/age checks and BeetrootBlock's separate 1-in-3 gate remain vanilla.
    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int regnum$growthChance(RandomSource random, int bound, BlockState state, ServerLevel level,
                                    BlockPos pos, RandomSource tickRandom) {
        if (!CropGrowthService.ownsCropTicks()) return random.nextInt(bound);
        double multiplier = CropGrowthService.getCropGrowthMultiplier(level, pos, state);
        if (multiplier == 1) return random.nextInt(bound);
        return random.nextDouble() < GrowthProbability.modified(bound, multiplier) ? 0 : 1;
    }
}
