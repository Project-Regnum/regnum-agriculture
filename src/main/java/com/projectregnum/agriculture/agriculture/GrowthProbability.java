// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.agriculture;

public final class GrowthProbability {
    private GrowthProbability() {}
    /** One opportunity to advance one age. Never schedules or re-enters a random tick. */
    public static double modified(int vanillaBound, double multiplier) {
        if (vanillaBound < 1) throw new IllegalArgumentException("Invalid vanilla random bound");
        if (!Double.isFinite(multiplier) || multiplier < 0) multiplier = 1;
        return Math.min(1, multiplier / vanillaBound);
    }
    public static boolean ownsCropTicks(boolean regnumWorldInstalled) { return !regnumWorldInstalled; }
}
