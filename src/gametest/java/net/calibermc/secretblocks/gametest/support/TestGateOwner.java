package net.calibermc.secretblocks.gametest.support;

import net.calibermc.secretblocks.blocks.GateSegmentOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A stand-in Secret Gate for the segment tests, simpler than the real
 * gate. Registered by the game-test mod only, never shipped. It keeps
 * segments while {@code powered}, on its line: the 9 blocks east of it.
 */
public class TestGateOwner extends Block implements GateSegmentOwner {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final int LENGTH = 9;

	public TestGateOwner(Properties settings) {
		super(settings);
		registerDefaultState(stateDefinition.any().setValue(POWERED, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	@Override
	public boolean keepsSegment(BlockState ownerState, Level level, BlockPos ownerPos, BlockPos segmentPos) {
		if (!ownerState.getValue(POWERED)) {
			return false;
		}
		for (int i = 1; i <= LENGTH; i++) {
			if (ownerPos.relative(Direction.EAST, i).equals(segmentPos)) {
				return true;
			}
		}
		return false;
	}
}
