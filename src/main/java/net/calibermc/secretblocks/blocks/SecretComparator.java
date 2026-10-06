package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretComparatorBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Secret Comparator: a vanilla comparator in a full disguised block.
 * Vanilla's comparator code is kept whole: compare and subtract modes
 * toggled by use, reading a container or (through a solid block) an item
 * frame behind it, side input, and the output stored in its block entity.
 * Vanilla asks for "a {@code ComparatorBlockEntity}" everywhere, so the block
 * entity is a subclass of it with the disguise added
 * ({@link SecretComparatorBlockEntity}).
 *
 * The game tells only vanilla comparators when a container's contents change;
 * {@code ComparatorUpdateMixin} includes this block.
 *
 * Differences from vanilla, so it can hide in a wall: it stays when the block
 * under it goes and it is a full block. Not a redstone conductor (set
 * in {@code ModBlocks}).
 */
public class SecretComparator extends ComparatorBlock implements SecretBlock {

	public SecretComparator(Properties settings) {
		super(settings);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretComparatorBlockEntity(pos, state);
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

	/** Stays without a block under it, so it can sit in a wall (vanilla pops). */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return true;
	}
}
