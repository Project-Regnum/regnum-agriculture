// SPDX-License-Identifier: MPL-2.0
package com.projectregnum.agriculture.climate;

import net.minecraft.core.BlockPos;

/** Internal mixin bridge to vanilla's position-adjusted temperature. */
public interface BiomeTemperatureAccess {
    float regnum$temperatureAt(BlockPos pos);
}
