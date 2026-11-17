package vectorwing.farmersdelight.common.mixin.regnum;

import com.projectregnum.agriculture.sync.RegnumCropSync;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppress only World's helper during bounded original FD ticks; never suppress the crop tick itself. */
@Pseudo
@Mixin(targets = "zooller.regnumworld.growth.GrowthHelper", remap = false)
public class WorldGrowthReplayMixin {
    @Inject(method = "handleRandomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void agriculture$originalTick(Block block, BlockState state, ServerLevel level,
            BlockPos pos, RandomSource random, CallbackInfo worldCallback, CallbackInfo ci) {
        if (RegnumCropSync.isReplaying()) ci.cancel();
    }
}
