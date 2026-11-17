package com.projectregnum.agriculture.sync;

import java.util.function.DoubleSupplier;

/** Bounded stochastic rounding, driven solely by the server's random source. */
public final class GrowthAttempts {
    private GrowthAttempts() {}

    public static int count(double rate, DoubleSupplier random) {
        if (!Double.isFinite(rate) || rate <= 0) return 0;
        double bounded = Math.min(16, rate);
        int whole = (int) bounded;
        double fraction = bounded - whole;
        return whole + (fraction > 0 && random.getAsDouble() < fraction ? 1 : 0);
    }
}
