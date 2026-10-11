package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;

import java.util.function.Predicate;

/**
 * Secret Pressure Plate and Secret Player Pressure Plate: a full, disguised
 * floor block that powers redstone while something stands on it, so the
 * trigger looks exactly like the floor around it. Which entities count is
 * the {@code trigger} predicate.
 *
 * Pressing happens when an entity stands on the top face ({@code stepOn};
 * sneaking players count too). While pressed it re-checks the space just
 * above its top face every second and releases once nothing that counts is
 * there. It gives power 15 to its neighbors (weak power: it
 * does not power the block under it through that block).
 */
public class PressurePlate extends Block implements EntityBlock, SecretBlock {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final int RECHECK_TICKS = 20;
	private static final double DETECTION_HEIGHT = 0.25;

	private final Predicate<Entity> trigger;

	public PressurePlate(Properties settings, Predicate<Entity> trigger) {
		// A piston breaks it, as it breaks a vanilla pressure plate.
		super(settings.pushReaction(PushReaction.DESTROY));
		this.trigger = trigger;
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
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

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (!level.isClientSide() && !state.getValue(POWERED) && counts(entity)) {
			update(level, pos, state);
		}
		super.stepOn(level, pos, state, entity);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(POWERED)) {
			update(level, pos, state);
		}
	}

	private boolean counts(Entity entity) {
		return !entity.isIgnoringBlockTriggers() && trigger.test(entity);
	}

	/** Something that counts is in the slice just above the top face (the block itself is solid). */
	private boolean pressed(Level level, BlockPos pos) {
		AABB above = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(),
				pos.getX() + 1, pos.getY() + 1 + DETECTION_HEIGHT, pos.getZ() + 1);
		return !level.getEntitiesOfClass(Entity.class, above, this::counts).isEmpty();
	}

	private void update(Level level, BlockPos pos, BlockState state) {
		boolean was = state.getValue(POWERED);
		boolean now = pressed(level, pos);
		if (now != was) {
			level.setBlock(pos, state.setValue(POWERED, now), Block.UPDATE_ALL);
			level.updateNeighborsAt(pos, this);
			level.playSound(null, pos, now ? SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON : SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF,
					SoundSource.BLOCKS, 0.3F, now ? 0.6F : 0.5F);
			level.gameEvent(null, now ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
		}
		if (now) {
			level.scheduleTick(pos, this, RECHECK_TICKS);
		}
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		boolean removed = !state.is(newState.getBlock());
		super.onRemove(state, level, pos, newState, moved);
		if (removed && state.getValue(POWERED)) {
			level.updateNeighborsAt(pos, this);
		}
	}

}
