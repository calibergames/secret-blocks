package net.calibermc.secretblocks.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;

public class SlabBlock extends net.minecraft.world.level.block.SlabBlock implements EntityBlock, SecretBlock {

	public SlabBlock(Properties settings) {
		super(settings);
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
	public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return (stateFrom.getBlock() instanceof SecretBlock) ? true : super.skipRendering(state, stateFrom, direction);
	}

	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		// The slab's own shape (a full block when double); the outline stays a full cube.
		return super.getShape(state, world, pos, context);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

}
