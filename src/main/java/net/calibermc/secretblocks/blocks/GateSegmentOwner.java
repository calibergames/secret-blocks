package net.calibermc.secretblocks.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that builds Secret Gate segments ({@link SecretGate}). A segment
 * asks the block at its stored owner position whether it still keeps it; if
 * the answer is no, or that block is not an owner at all, the segment removes
 * itself ({@link SecretGateSegment}).
 */
public interface GateSegmentOwner {

	/** The most segments a gate builds in a line (9, as in the original). A segment further away has no owner. */
	int MAX_LENGTH = 9;

	/**
	 * Whether this owner, in {@code ownerState} at {@code ownerPos}, keeps a
	 * segment at {@code segmentPos} now: false when it is unpowered, or when
	 * {@code segmentPos} is not one of its segments' places (a segment copied
	 * there by {@code /clone}, for one). The gate must answer false beyond the
	 * line it has actually built (a blocked line has fewer than
	 * {@link #MAX_LENGTH}), so a copy placed past an obstruction goes too.
	 */
	boolean keepsSegment(BlockState ownerState, Level level, BlockPos ownerPos, BlockPos segmentPos);

	/** One of its segments at {@code segmentPos} was removed (a player broke it, or it removed itself). */
	default void segmentRemoved(BlockState ownerState, Level level, BlockPos ownerPos, BlockPos segmentPos) {
	}
}
