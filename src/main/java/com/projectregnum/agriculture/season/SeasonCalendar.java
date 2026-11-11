// SPDX-License-Identifier: MPL-2.0
// Calendar arithmetic adapted from FabricSeasons.getCurrentSeason, revision e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c.
// Original authors: D4rkness_King and Fabric Seasons contributors. See THIRD_PARTY_NOTICES.md.
package com.projectregnum.agriculture.season;

/** Pure calendar arithmetic. Public day numbers are one-based. */
public final class SeasonCalendar {
    public static final long TICKS_PER_DAY = 24_000L;
    public static final int MAX_LENGTH_DAYS = 36_500;
    private SeasonCalendar() {}

    public static long seasonTicks(int days) {
        if (days < 1 || days > MAX_LENGTH_DAYS) throw new IllegalArgumentException("Invalid season length");
        return days * TICKS_PER_DAY;
    }

    public static long cycleTick(long time, long anchorTime, long anchorPhase, int days) {
        long year = 4 * seasonTicks(days);
        // Reduce before adding/subtracting to avoid overflow for arbitrary saved world times.
        return Math.floorMod(Math.floorMod(time, year) - Math.floorMod(anchorTime, year)
                + Math.floorMod(anchorPhase, year), year);
    }

    public static Snapshot at(long time, long anchorTime, long anchorPhase, int days) {
        return fromCycleTick(cycleTick(time, anchorTime, anchorPhase, days), days);
    }

    public static Snapshot fromCycleTick(long tick, int days) {
        long length = seasonTicks(days);
        tick = Math.floorMod(tick, 4 * length);
        long within = tick % length;
        return new Snapshot(Season.values()[(int) (tick / length)],
                (int) (within / TICKS_PER_DAY) + 1, Math.min(Math.nextDown(1.0f), (float) ((double) within / length)), within);
    }

    public static long resize(long tick, int oldDays, int newDays) {
        long oldLength = seasonTicks(oldDays);
        tick = Math.floorMod(tick, oldLength * 4);
        long newLength = seasonTicks(newDays);
        return (tick / oldLength) * newLength + (tick % oldLength) * newLength / oldLength;
    }

    public record Snapshot(Season season, int day, float progress, long ticksWithinSeason) {}
}
