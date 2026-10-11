package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StairBlock extends net.minecraft.world.level.block.StairBlock implements EntityBlock, SecretBlock {

	public StairBlock(BlockState baseBlockState, Properties settings) {
		super(baseBlockState, settings);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretBlockEntity(pos, state);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (world.getBlockEntity(pos) instanceof SecretBlockEntity) {
			SecretBlockEntity blockEntity = (SecretBlockEntity) world.getBlockEntity(pos);
		}
		return super.use(state, world, pos, player, hand, hit);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return (stateFrom.getBlock() instanceof SecretBlock) ? true : super.skipRendering(state, stateFrom, direction);
	}


	@Override  // COLLIDE ONLY WITH STAIR SHAPE
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return (state.getValue(HALF) == Half.TOP ? TOP_SHAPES : BOTTOM_SHAPES)[SHAPE_INDICES[this.getShapeIndex(state)]];

	}

	@Override  // OUTLINE FULL CUBE
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.block();

	}

	@Override
	public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
		super.onRemove(state, world, pos, newState, moved);
		// StairBlock.onRemove hands removal to its base block, which removes this
		// block entity only because the base (the Dummy Block) has one too.
		if (!state.is(newState.getBlock())) {
			world.removeBlockEntity(pos);
		}
	}

    //Gets the stair shape for collision
	private static final int[] SHAPE_INDICES;  // for stairs shape

	private int getShapeIndex(BlockState state) {
		return ((StairsShape)state.getValue(SHAPE)).ordinal() * 4 + ((Direction)state.getValue(FACING)).get2DDataValue();
	}

	private static StairsShape getStairsShape(BlockState state, BlockGetter world, BlockPos pos) {
		Direction direction = (Direction)state.getValue(FACING);
		BlockState blockState = world.getBlockState(pos.relative(direction));
		if (isStairs(blockState) && state.getValue(HALF) == blockState.getValue(HALF)) {
			Direction direction2 = (Direction)blockState.getValue(FACING);
			if (direction2.getAxis() != ((Direction)state.getValue(FACING)).getAxis() && canTakeShape(state, world, pos, direction2.getOpposite())) {
				if (direction2 == direction.getCounterClockWise()) {
					return StairsShape.OUTER_LEFT;
				}

				return StairsShape.OUTER_RIGHT;
			}
		}

		BlockState blockState2 = world.getBlockState(pos.relative(direction.getOpposite()));
		if (isStairs(blockState2) && state.getValue(HALF) == blockState2.getValue(HALF)) {
			Direction direction3 = (Direction)blockState2.getValue(FACING);
			if (direction3.getAxis() != ((Direction)state.getValue(FACING)).getAxis() && canTakeShape(state, world, pos, direction3)) {
				if (direction3 == direction.getCounterClockWise()) {
					return StairsShape.INNER_LEFT;
				}

				return StairsShape.INNER_RIGHT;
			}
		}

		return StairsShape.STRAIGHT;
	}

	private static boolean canTakeShape(BlockState state, BlockGetter world, BlockPos pos, Direction dir) {
		BlockState blockState = world.getBlockState(pos.relative(dir));
		return !isStairs(blockState) || blockState.getValue(FACING) != state.getValue(FACING) || blockState.getValue(HALF) != state.getValue(HALF);
	}

	static {
		SHAPE_INDICES = new int[]{12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};
	}

}