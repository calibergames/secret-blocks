package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Secret Trapped Chest: a Secret Chest that gives a redstone signal equal to
 * the number of players looking into it (at most 15), as a vanilla trapped
 * chest does: weak power on every side and strong power into the block below.
 *
 * The count is kept by the chest's block entity
 * ({@link SecretChestBlockEntity}, shared with the Secret Chest), which tells
 * the neighbours when it changes. Not a redstone conductor (set in
 * {@code ModBlocks}).
 */
public class SecretTrappedChest extends SecretChest {

	public SecretTrappedChest(Properties settings) {
		super(settings);
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof SecretChestBlockEntity chest ? Mth.clamp(chest.getOpenerCount(), 0, 15) : 0;
	}

	/** Strong power only into the block below (asked from below, the direction is UP), as vanilla. */
	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return direction == Direction.UP ? state.getSignal(level, pos, direction) : 0;
	}
}
