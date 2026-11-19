package com.projectregnum.agriculture.sync;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class RegnumCropSync {
    private static boolean regnumWorldLoaded = false;
    private static Method isUndergroundGrowthBlockedMethod = null;
    private static Method getFinalCropMultiplierMethod = null;
    private static Method registerGrowthControllerMethod = null;
    private static Class<?> externalGrowthControllerClass = null;
    private static Class<?> growthResultClass = null;

    public static void init() {
        if (!FabricLoader.getInstance().isModLoaded("regnum-world")) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("zooller.regnumworld.api.RegnumWorldApi");
            externalGrowthControllerClass = Class.forName("zooller.regnumworld.api.ExternalGrowthController");
            growthResultClass = Class.forName("zooller.regnumworld.api.GrowthResult");

            isUndergroundGrowthBlockedMethod = apiClass.getMethod("isUndergroundGrowthBlocked", Level.class, BlockPos.class);
            getFinalCropMultiplierMethod = apiClass.getMethod("getFinalCropMultiplier", ServerLevel.class, BlockPos.class, BlockState.class);
            registerGrowthControllerMethod = apiClass.getMethod("registerGrowthController", externalGrowthControllerClass);

            Object controllerProxy = Proxy.newProxyInstance(
                    RegnumCropSync.class.getClassLoader(),
                    new Class<?>[]{externalGrowthControllerClass},
                    (proxy, method, args) -> {
                        if ("onRandomTick".equals(method.getName())) {
                            return Enum.valueOf((Class<Enum>) growthResultClass, "PASS");
                        }
                        if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        }
                        if ("toString".equals(method.getName())) {
                            return "RegnumAgricultureGrowthController";
                        }
                        return null;
                    }
            );

            registerGrowthControllerMethod.invoke(null, controllerProxy);
            regnumWorldLoaded = true;
        } catch (Throwable ignored) {
            regnumWorldLoaded = false;
        }
    }

    public static boolean isRegnumWorldLoaded() {
        return regnumWorldLoaded;
    }

    public static boolean isUndergroundBlocked(Level level, BlockPos pos) {
        if (!regnumWorldLoaded || level == null || isUndergroundGrowthBlockedMethod == null) {
            return false;
        }
        try {
            return (Boolean) isUndergroundGrowthBlockedMethod.invoke(null, level, pos);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static double getMultiplier(ServerLevel level, BlockPos pos, BlockState state) {
        if (!regnumWorldLoaded || level == null || getFinalCropMultiplierMethod == null) {
            return 1.0;
        }
        try {
            return (Double) getFinalCropMultiplierMethod.invoke(null, level, pos, state);
        } catch (Throwable ignored) {
            return 1.0;
        }
    }

    public static boolean shouldCancelGrowth(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (!regnumWorldLoaded || level == null) {
            return false;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }
        if (isUndergroundBlocked(level, pos)) {
            return true;
        }
        double multiplier = getMultiplier(level, pos, state);
        if (multiplier <= 0.0) {
            return true;
        }
        if (multiplier < 1.0) {
            return random.nextDouble() > multiplier;
        }
        return false;
    }
}
