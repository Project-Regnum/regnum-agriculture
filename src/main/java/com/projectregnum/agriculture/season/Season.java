// SPDX-License-Identifier: MPL-2.0
// Adapted from Fabric Seasons utils/Season.java, e0b2dd285e5a7f0b2850871ea7a0eb5289d0937c.
// Original authors: D4rkness_King and Fabric Seasons contributors. See THIRD_PARTY_NOTICES.md.
package com.projectregnum.agriculture.season;

import java.util.Locale;

public enum Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    public Season next() { return values()[(ordinal() + 1) % 4]; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "season.regnum_agriculture." + id(); }
}
