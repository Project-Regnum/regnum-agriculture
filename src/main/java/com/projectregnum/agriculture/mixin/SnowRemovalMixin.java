// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.mixin;

import com.projectregnum.agriculture.climate.SeasonalSnowState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class SnowRemovalMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void regnum$forgetSnow(BlockState state, Level level, BlockPos pos, BlockState next,
                                   boolean moving, CallbackInfo ci) {
        if (state.is(Blocks.SNOW) && !next.is(Blocks.SNOW) && level instanceof ServerLevel server)
            SeasonalSnowState.get(server).remove(pos);
    }
}
