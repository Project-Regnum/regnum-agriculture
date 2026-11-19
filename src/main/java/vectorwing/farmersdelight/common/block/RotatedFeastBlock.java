package vectorwing.farmersdelight.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import vectorwing.farmersdelight.FarmersDelight;
import vectorwing.farmersdelight.common.utility.ShapeUtils;

import java.util.function.Supplier;

public class RotatedFeastBlock extends FeastBlock
{
	private final VoxelShape[][] combinedShapes;

	public RotatedFeastBlock(Properties properties, Supplier<Item> servingItem, boolean hasLeftovers, VoxelShape[] feastShapes, @Nullable VoxelShape containerShape) {
		super(properties, servingItem, hasLeftovers, true);
		this.combinedShapes = containerShape == null ? ShapeUtils.buildRotatedFoodShapes(feastShapes) : ShapeUtils.buildPlatedFoodShapes(feastShapes, containerShape);
	}

	public RotatedFeastBlock(Properties properties, String servingItem, boolean hasLeftovers, VoxelShape[] feastShapes, @Nullable VoxelShape containerShape) {
		super(properties, servingItem, hasLeftovers, true);
		this.combinedShapes = containerShape == null ? ShapeUtils.buildRotatedFoodShapes(feastShapes) : ShapeUtils.buildPlatedFoodShapes(feastShapes, containerShape);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return combinedShapes[state.getValue(SERVINGS)][state.getValue(FACING).get2DDataValue()];
	}
}
