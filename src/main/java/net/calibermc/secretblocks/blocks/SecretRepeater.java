package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Secret Repeater: a vanilla repeater in a full disguised block. Vanilla's diode logic is kept
 * whole: facing from placement, delay 1–4 cycled by use (only when the player
 * may build), 2-tick steps, locking by a powered diode at its side (vanilla
 * and Secret diodes lock each other, both being {@code DiodeBlock}s).
 *
 * Differences from vanilla, so it can hide in a wall: it stays when the block
 * under it goes, it is a full block, and it gives off no redstone particles
 * when powered (they would give it away). Not a redstone conductor (set in
 * {@code ModBlocks}).
 */
public class SecretRepeater extends RepeaterBlock implements EntityBlock, SecretBlock {

	public SecretRepeater(Properties settings) {
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

	/** No redstone particles when powered: they would show where it is. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
	}

	/** A piston breaks it, as it breaks a vanilla repeater. */
	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.DESTROY;
	}
}
