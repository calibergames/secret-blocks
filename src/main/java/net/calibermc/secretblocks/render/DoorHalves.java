package net.calibermc.secretblocks.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Whether a door half meets its other half, so the Builder's Goggles'
 * outline draws the two as one panel. No client classes, so the game tests
 * can check it.
 */
public final class DoorHalves {

	private DoorHalves() {
	}

	/** A lower half with its upper half (the same block) right above it. */
	public static boolean joinsAbove(BlockGetter level, BlockPos pos, BlockState state) {
		return isHalf(state, DoubleBlockHalf.LOWER) && isHalfOf(level.getBlockState(pos.above()), state, DoubleBlockHalf.UPPER);
	}

	/** An upper half with its lower half (the same block) right below it. */
	public static boolean joinsBelow(BlockGetter level, BlockPos pos, BlockState state) {
		return isHalf(state, DoubleBlockHalf.UPPER) && isHalfOf(level.getBlockState(pos.below()), state, DoubleBlockHalf.LOWER);
	}

	private static boolean isHalf(BlockState state, DoubleBlockHalf half) {
		return state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == half;
	}

	private static boolean isHalfOf(BlockState other, BlockState state, DoubleBlockHalf half) {
		return other.is(state.getBlock()) && isHalf(other, half);
	}
}
