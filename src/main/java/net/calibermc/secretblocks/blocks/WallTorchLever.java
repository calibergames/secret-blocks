package net.calibermc.secretblocks.blocks;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Torch Lever on a wall: a vanilla wall torch that also toggles like a lever
 * ({@link TorchLevers}; see {@link TorchLever}). It gives strong power into
 * the wall block it hangs on. Switched on it leans further from the wall:
 * 45 degrees where a wall torch leans 22.5.
 */
public class WallTorchLever extends WallTorchBlock {

	/** Its outline while on: the vanilla wall torch's box, reaching 8.5 from the wall instead of 5. */
	private static final Map<Direction, VoxelShape> TILTED = Map.of(
			Direction.NORTH, Block.box(5.5, 3.0, 7.5, 10.5, 13.0, 16.0),
			Direction.SOUTH, Block.box(5.5, 3.0, 0.0, 10.5, 13.0, 8.5),
			Direction.WEST, Block.box(7.5, 3.0, 5.5, 16.0, 13.0, 10.5),
			Direction.EAST, Block.box(0.0, 3.0, 5.5, 8.5, 13.0, 10.5));

	public WallTorchLever(Properties settings) {
		super(settings, ParticleTypes.FLAME);
		registerDefaultState(defaultBlockState().setValue(TorchLevers.POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(TorchLevers.POWERED);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(TorchLevers.POWERED) ? TILTED.get(state.getValue(FACING)) : super.getShape(state, level, pos, context);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(TorchLevers.POWERED)) {
			super.animateTick(state, level, pos, random);
			return;
		}
		TorchLevers.tiltedFlame(level, flameParticle, pos, tiltedFlame(state.getValue(FACING)));
	}

	/**
	 * Where the flame appears in the block while on: the tip of the tilted torch.
	 * Vanilla's flame is about 11.8 pixels from the stem's pivot at the wall;
	 * this is the same distance along the axis of a stem leaning 45 degrees,
	 * which is 0.52 out from the wall and 0.74 up.
	 */
	public static Vec3 tiltedFlame(Direction facing) {
		return new Vec3(0.5 + 0.02 * facing.getStepX(), 0.74, 0.5 + 0.02 * facing.getStepZ());
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return TorchLevers.use(state, level, pos, player, state.getValue(FACING));
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return TorchLevers.signal(state);
	}

	@Override
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return TorchLevers.directSignal(state, direction, state.getValue(FACING));
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		TorchLevers.removed(state, level, pos, newState, moved, state.getValue(FACING));
		super.onRemove(state, level, pos, newState, moved);
	}
}
