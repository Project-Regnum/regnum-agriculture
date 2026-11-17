package vectorwing.farmersdelight.common.mixin.refabricated;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import vectorwing.farmersdelight.common.block.RichSoilBlock;
import vectorwing.farmersdelight.common.block.RichSoilFarmlandBlock;
import vectorwing.farmersdelight.common.utility.SoilUtils;

@Mixin(CropBlock.class)
public class CropBlockMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "growCrops", at = @At("HEAD"), cancellable = true)
    private void agriculture$restrictDirectGrowth(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && (com.projectregnum.agriculture.sync.RegnumCropSync.isUndergroundBlocked(server, pos)
                || com.projectregnum.agriculture.sync.RegnumCropSync.getMultiplier(server, pos, state) <= 0)) ci.cancel();
    }

    @ModifyVariable(method = "getGrowthSpeed", at = @At(value = "LOAD", ordinal = 1), ordinal = 1)
    private static float farmersdelightrefabricated$modifyGrowthSpeedForNonFarmland(float original, Block block, BlockGetter level, BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        if (belowState.getBlock() instanceof RichSoilBlock && SoilUtils.isAbleToPlaceRichSoil(block) && original < 0.00001F)
            return 1.0F;

        if (belowState.getBlock() instanceof RichSoilFarmlandBlock && SoilUtils.isAbleToPlaceRichSoilFarmland(block) && original < 0.00001F) {
            if (belowState.hasProperty(RichSoilFarmlandBlock.MOISTURE) && belowState.getValue(RichSoilFarmlandBlock.MOISTURE) > 0)
                return 3.0F;
            return 1.0F;
        }

        return original;
    }
}
