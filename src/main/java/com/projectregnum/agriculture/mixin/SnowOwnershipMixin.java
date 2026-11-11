// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.mixin;

import com.projectregnum.agriculture.climate.SeasonalSnowState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class SnowOwnershipMixin {
    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    private void regnum$playerSnow(Level level, BlockPos pos, BlockState state, LivingEntity placer,
                                  ItemStack stack, CallbackInfo ci) {
        if (state.is(Blocks.SNOW) && level instanceof ServerLevel server)
            SeasonalSnowState.get(server).remove(pos);
    }
}
