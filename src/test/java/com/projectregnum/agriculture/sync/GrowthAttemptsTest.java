package com.projectregnum.agriculture.sync;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GrowthAttemptsTest {
    @Test void boundariesAndInvalidRates() {
        for (double rate : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY})
            assertEquals(0, GrowthAttempts.count(rate, () -> { throw new AssertionError("Unexpected RNG"); }));
        assertEquals(1, GrowthAttempts.count(1, () -> { throw new AssertionError("Neutral rates preserve RNG"); }));
        assertEquals(16, GrowthAttempts.count(1000, () -> 0));
    }
    @Test void fractionsAndTransitionUseCurrentRate() {
        assertEquals(0, GrowthAttempts.count(0.25, () -> 0.25));
        assertEquals(1, GrowthAttempts.count(0.25, () -> 0.249));
        assertEquals(2, GrowthAttempts.count(2.5, () -> 0.5));
        assertEquals(3, GrowthAttempts.count(2.5, () -> 0.49));
        assertEquals(0, GrowthAttempts.count(0, () -> 0));
    }
}
