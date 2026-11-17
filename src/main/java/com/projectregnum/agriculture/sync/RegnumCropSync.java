package com.projectregnum.agriculture.sync;

import com.projectregnum.agriculture.RegnumAgriculture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Optional World bridge. Keeps API access isolated and never invents season data. */
public final class RegnumCropSync {
    private static Method underground;
    private static Method multiplier;
    private static boolean active;
    private static boolean initialized;
    private static boolean failureReported;
    private static final ThreadLocal<Boolean> REPLAY = ThreadLocal.withInitial(() -> false);

    private RegnumCropSync() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void init() {
        if (initialized) return;
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded("regnum-world")) {
            RegnumAgriculture.LOGGER.info("Regnum World absent: retaining standalone Farmer's Delight growth.");
            return;
        }
        try {
            Class<?> api = Class.forName("zooller.regnumworld.api.RegnumWorldApi");
            Class<?> controller = Class.forName("zooller.regnumworld.api.ExternalGrowthController");
            Class<? extends Enum> result = (Class<? extends Enum>) Class.forName("zooller.regnumworld.api.GrowthResult");
            Object pass = Enum.valueOf(result, "PASS");
            Object handled = Enum.valueOf(result, "HANDLED");
            underground = api.getMethod("isUndergroundGrowthBlocked", Level.class, BlockPos.class);
            multiplier = api.getMethod("getFinalCropMultiplier", ServerLevel.class, BlockPos.class, BlockState.class);
            Object bridge = Proxy.newProxyInstance(RegnumCropSync.class.getClassLoader(), new Class<?>[]{controller},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "onRandomTick" -> {
                            BlockState state = (BlockState) args[0];
                            ServerLevel level = (ServerLevel) args[1];
                            if (!active || isReplaying() || !isImportedCrop(state)) yield pass;
                            replay(state, level, (BlockPos) args[2], (RandomSource) args[3], ((Number) args[4]).doubleValue());
                            yield handled;
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "RegnumAgricultureGrowthController";
                        default -> throw new UnsupportedOperationException(method.toString());
                    });
            api.getMethod("registerGrowthController", controller).invoke(null, bridge);
            active = true;
            RegnumAgriculture.LOGGER.info("Regnum World growth bridge active. This API supplies regional rules; no season calendar is exposed.");
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException e) {
            reportFailure(e);
        }
    }

    private static void reportFailure(Throwable error) {
        active = false;
        if (!failureReported) {
            failureReported = true;
            RegnumAgriculture.LOGGER.error("Regnum World growth integration unavailable. Agriculture's bridge is disabled; check World API compatibility. World may still apply its own rules.", error);
        }
    }

    public static boolean isRegnumWorldLoaded() { return active; }
    public static boolean isReplaying() { return REPLAY.get(); }

    public static boolean isImportedCrop(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("farmersdelight");
    }

    public static boolean isUndergroundBlocked(Level level, BlockPos pos) {
        if (!active || level == null || level.dimension() != Level.OVERWORLD) return false;
        try {
            return (Boolean) underground.invoke(null, level, pos);
        } catch (ReflectiveOperationException | ClassCastException e) {
            reportFailure(e);
            return false;
        }
    }

    public static double getMultiplier(ServerLevel level, BlockPos pos, BlockState state) {
        if (!active || level == null || level.dimension() != Level.OVERWORLD) return 1;
        try {
            double value = ((Number) multiplier.invoke(null, level, pos, state)).doubleValue();
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid World multiplier: " + value);
            return value;
        } catch (ReflectiveOperationException | ClassCastException | IllegalArgumentException e) {
            reportFailure(e);
            return 1;
        }
    }

    /** For source overrides that never enter World's vanilla CropBlock mixin. */
    public static boolean handleRandomTick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (!active || isReplaying() || level.dimension() != Level.OVERWORLD) return false;
        double rate = isUndergroundBlocked(level, pos) ? 0 : getMultiplier(level, pos, state);
        replay(state, level, pos, random, rate);
        return true;
    }

    private static void replay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, double rate) {
        int attempts = GrowthAttempts.count(rate, random::nextDouble);
        REPLAY.set(true);
        try {
            for (int i = 0; i < attempts; i++) {
                BlockState current = level.getBlockState(pos);
                // Stop after a crop transforms: do not tick an unrelated replacement.
                if (current.getBlock() != state.getBlock() || !current.isRandomlyTicking()) break;
                current.randomTick(level, pos, random);
            }
        } finally {
            REPLAY.remove();
        }
    }

    /** Rich soil is a separate growth opportunity, not an extra random-tick multiplier. */
    public static boolean shouldCancelGrowth(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (isReplaying()) return false;
        if (isUndergroundBlocked(level, pos)) return true;
        double rate = getMultiplier(level, pos, state);
        return rate <= 0 || (rate < 1 && random.nextDouble() >= rate);
    }
}
