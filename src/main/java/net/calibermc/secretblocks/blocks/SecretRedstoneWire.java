package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Secret Redstone Wire: the original mod's design, our own code. A full
 * disguised block that carries a signal on all six sides, so a column carries
 * it up and down and a line runs through a wall in any direction.
 *
 * <ul>
 *   <li>Its power is the strongest direct signal on any side, or the
 *   strongest neighbouring wire (vanilla wire, Secret Redstone Dust or Wire,
 *   any of the six sides) minus 1.</li>
 *   <li>It gives weak power, its power level, on every side and never strong
 *   power (it lights a lamp touching any face, but does not power through a
 *   block). It is not a conductor.</li>
 *   <li>It is one of the wire family ({@link RedstoneWireFamily}): it uses
 *   vanilla's own {@code POWER} property, and while any wire computes its
 *   power it gives no signal, so wires read each other only by power level
 *   and cannot latch.</li>
 * </ul>
 */
public class SecretRedstoneWire extends Block implements EntityBlock, SecretBlock {

	/** Vanilla's own property object: vanilla reads it from any wire of the family. */
	public static final IntegerProperty POWER = RedStoneWireBlock.POWER;

	public SecretRedstoneWire(Properties settings) {
		super(settings);
		registerDefaultState(stateDefinition.any().setValue(POWER, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWER);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return stateFrom.getBlock() instanceof SecretBlock || super.skipRendering(state, stateFrom, direction);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	// --- signal ------------------------------------------------------------

	@Override
	public boolean isSignalSource(BlockState state) {
		return RedstoneWireFamily.signalling;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return RedstoneWireFamily.signalling ? state.getValue(POWER) : 0;
	}

	/**
	 * Weak power only, as dust: no strong power into the blocks around it.
	 * The one exception is a repeater or comparator beside it: a comparator
	 * reads its side inputs as direct signals (vanilla names only its own
	 * dust there), so the Wire gives its power to diodes that ask, as dust
	 * would. Diodes are not conductors, so nothing else is powered by it.
	 */
	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		BlockState asking = level.getBlockState(pos.relative(direction.getOpposite()));
		return asking.getBlock() instanceof DiodeBlock ? getSignal(state, level, pos, direction) : 0;
	}

	// --- updates -----------------------------------------------------------

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
		if (!oldState.is(this) && !level.isClientSide) {
			updatePower(level, pos, state);
			notifyAround(level, pos);
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			super.onRemove(state, level, pos, newState, moved);
			if (!moved && !level.isClientSide) {
				notifyAround(level, pos);
			}
		}
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
		if (!level.isClientSide) {
			updatePower(level, pos, state);
		}
	}

	private void updatePower(Level level, BlockPos pos, BlockState state) {
		int target = targetStrength(level, pos);
		if (state.getValue(POWER) != target) {
			if (level.getBlockState(pos) == state) {
				level.setBlock(pos, state.setValue(POWER, target), Block.UPDATE_CLIENTS);
			}
			notifyAround(level, pos);
		}
	}

	/** The strongest direct signal on any side, or the strongest neighbouring wire minus 1. */
	private static int targetStrength(Level level, BlockPos pos) {
		int fromSources;
		RedstoneWireFamily.signalling = false;
		try {
			fromSources = level.getBestNeighborSignal(pos);
		} finally {
			RedstoneWireFamily.signalling = true;
		}
		if (fromSources >= 15) {
			return 15;
		}
		int fromWires = 0;
		for (Direction direction : Direction.values()) {
			BlockState neighbour = level.getBlockState(pos.relative(direction));
			if (RedstoneWireFamily.isWire(neighbour)) {
				fromWires = Math.max(fromWires, neighbour.getValue(POWER));
			}
		}
		return Math.max(fromSources, fromWires - 1);
	}

	/** Update the neighbours and their neighbours, so wires one step up or down hear the change too. */
	private void notifyAround(Level level, BlockPos pos) {
		level.updateNeighborsAt(pos, this);
		for (Direction direction : Direction.values()) {
			level.updateNeighborsAt(pos.relative(direction), this);
		}
	}
}
