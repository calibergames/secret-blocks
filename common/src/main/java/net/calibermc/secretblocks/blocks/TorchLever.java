package net.calibermc.secretblocks.blocks;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Torch Lever, standing on a floor (the original mod's design, our own
 * code). Not a disguised block: it is a vanilla torch in every way (model,
 * flame, light, instant break, popping off when its floor goes) that also
 * toggles like a lever when used ({@link TorchLevers}). Switched on it leans
 * 22.5 degrees the way the player faced when placing it (away from them);
 * Torch Levers saved before it had a direction lean east, as they always did.
 * {@link WallTorchLever} is the wall form; one item places both.
 */
public class TorchLever extends TorchBlock {

	/** The way it leans when on (the player's facing when placed). Default east: the only way before it had one. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Its outline while on: the vanilla torch's box, widened 3 pixels the way it leans. */
	private static final Map<Direction, VoxelShape> TILTED = Map.of(
			Direction.EAST, Block.box(6.0, 0.0, 6.0, 13.0, 10.0, 10.0),
			Direction.WEST, Block.box(3.0, 0.0, 6.0, 10.0, 10.0, 10.0),
			Direction.SOUTH, Block.box(6.0, 0.0, 6.0, 10.0, 10.0, 13.0),
			Direction.NORTH, Block.box(6.0, 0.0, 3.0, 10.0, 10.0, 10.0));

	public TorchLever(Properties settings) {
		super(settings, ParticleTypes.FLAME);
		registerDefaultState(stateDefinition.any().setValue(TorchLevers.POWERED, false).setValue(FACING, Direction.EAST));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TorchLevers.POWERED, FACING);
	}

	/** It leans away from the player who placed it. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
	 * Where the flame appears in the block while on: the tip of the tilted
	 * torch. Vanilla's flame sits 0.7 above the foot; this is the same distance
	 * along a stem leaning 22.5 degrees: 0.27 out the way it leans, 0.65 up.
	 */
	public static Vec3 tiltedFlame(Direction facing) {
		return new Vec3(0.5 + 0.27 * facing.getStepX(), 0.65, 0.5 + 0.27 * facing.getStepZ());
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return TorchLevers.use(state, level, pos, player, Direction.UP);
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
		return TorchLevers.directSignal(state, direction, Direction.UP);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		TorchLevers.removed(state, level, pos, newState, moved, Direction.UP);
		super.onRemove(state, level, pos, newState, moved);
	}
}
