package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The head of a Secret Piston: a full block that wears the base's disguise,
 * copied to it at the end of the move that builds it. Otherwise
 * vanilla's head: it needs its base (it goes when the base does, and breaking
 * it breaks the base), and pick-block gives the piston. No item, no drops.
 */
public class SecretPistonHead extends PistonHeadBlock implements EntityBlock, SecretBlock {

	public SecretPistonHead(Properties settings) {
		super(settings);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretBlockEntity(pos, state);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return false;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return stateFrom.getBlock() instanceof SecretBlock || super.skipRendering(state, stateFrom, direction);
	}

	/** Pistons can neither push nor break it, as vanilla's head (although many secret blocks can be pushed). */
	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.BLOCK;
	}

	/** Pick-block gives the piston it belongs to, as vanilla's head does. */
	@Override
	public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
		return new ItemStack(state.getValue(TYPE) == PistonType.STICKY ? ModBlocks.SECRET_STICKY_PISTON : ModBlocks.SECRET_PISTON);
	}
}
