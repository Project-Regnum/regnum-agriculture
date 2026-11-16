package vectorwing.farmersdelight.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.PackOutput;
import vectorwing.farmersdelight.FarmersDelight;

@Deprecated
public abstract class BlockStates extends FabricModelProvider
{
	private static final int DEFAULT_ANGLE_OFFSET = 180;

	public BlockStates(FabricDataOutput output) {
		super(output);
	}

}
