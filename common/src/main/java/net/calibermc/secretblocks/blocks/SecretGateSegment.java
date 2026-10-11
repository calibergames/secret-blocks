package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretGateSegmentBlockEntity;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Secret Gate segment: one block of the line a powered Secret Gate builds,
 * drawn as the gate's disguise. It has no item, drops nothing (so it cannot be
 * farmed, as the original's Dummy Blocks could), cannot be picked with
 * pick-block and cannot be pushed. Wool-like toughness. A player can break
 * it; the gate builds it again while open.
 *
 * Its block entity keeps the owner gate's position, server-side only. On its
 * first tick after it is placed or loaded, and once a second after that, it
 * asks its owner ({@link GateSegmentOwner}) whether it is still kept, and
 * removes itself if not: the owner is gone, is not a gate, is unpowered, or
 * does not keep a segment here. Orphans come from {@code /setblock},
 * {@code /clone} or a crash. While the owner's chunk is not loaded it stays
 * (nothing is loaded to find out), but only if the owner is within a gate's
 * reach of it.
 */
public class SecretGateSegment extends Block implements EntityBlock, SecretBlock {

	public SecretGateSegment(Properties settings) {
		// Pistons can neither push nor break it, though they push some other secret
		// blocks (SecretPushing).
		super(settings.pushReaction(PushReaction.BLOCK));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretGateSegmentBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() || type != ModEntities.SECRET_GATE_SEGMENT ? null
				: (BlockEntityTicker<T>) (BlockEntityTicker<SecretGateSegmentBlockEntity>) SecretGateSegmentBlockEntity::serverTick;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public boolean skipRendering(BlockState state, BlockState stateFrom, Direction direction) {
		return stateFrom.getBlock() instanceof SecretBlock || super.skipRendering(state, stateFrom, direction);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	/** Tell the owner, so a gate that is open can build it again. */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!level.isClientSide() && !state.is(newState.getBlock())
				&& level.getBlockEntity(pos) instanceof SecretGateSegmentBlockEntity segment && segment.getOwner() != null
				&& level.isLoaded(segment.getOwner())) {
			BlockState ownerState = level.getBlockState(segment.getOwner());
			if (ownerState.getBlock() instanceof GateSegmentOwner owner) {
				owner.segmentRemoved(ownerState, level, segment.getOwner(), pos);
			}
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	/** No pick-block: there is no item to pick (vanilla's would be empty too; this says so). */
	@Override
	public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
		return ItemStack.EMPTY;
	}

}
