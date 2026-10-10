package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;

/**
 * Secret Light and Heavy Weighted Pressure Plates: full, disguised floor
 * blocks whose signal grows with the number of entities standing on them,
 * as vanilla's weighted plates do. The light plate gives one level per
 * entity, up to 15; the heavy plate one level per ten entities, rounded up,
 * so 150 give 15. Every entity counts except spectators, as with vanilla's.
 *
 * Pressing happens when an entity stands on the top face ({@code stepOn}).
 * While pressed it counts again every 10 ticks, as vanilla's weighted plates,
 * and releases once nothing is there. Weak power only: it powers its
 * neighbours, not the block under it through that block.
 */
public class WeightedPressurePlate extends Block implements EntityBlock, SecretBlock {

	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	private static final int RECHECK_TICKS = 10;
	private static final double DETECTION_HEIGHT = 0.25;

	/** The number of entities that gives a full signal: 15 for the light plate, 150 for the heavy one. */
	private final int maxWeight;

	public WeightedPressurePlate(Properties settings, int maxWeight) {
		// A piston breaks it, as it breaks a vanilla pressure plate.
		super(settings.pushReaction(PushReaction.DESTROY));
		this.maxWeight = maxWeight;
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
		return (stateFrom.getBlock() instanceof SecretBlock) ? true : super.skipRendering(state, stateFrom, direction);
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (!level.isClientSide() && state.getValue(POWER) == 0) {
			update(level, pos, state);
		}
		super.stepOn(level, pos, state, entity);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(POWER) > 0) {
			update(level, pos, state);
		}
	}

	/** The signal for the entities in the slice just above the top face (the block itself is solid). */
	int strength(Level level, BlockPos pos) {
		AABB above = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(),
				pos.getX() + 1, pos.getY() + 1 + DETECTION_HEIGHT, pos.getZ() + 1);
		int count = Math.min(level.getEntitiesOfClass(Entity.class, above).size(), maxWeight);
		return count > 0 ? Mth.ceil((float) count / maxWeight * 15.0F) : 0;
	}

	private void update(Level level, BlockPos pos, BlockState state) {
		int was = state.getValue(POWER);
		int now = strength(level, pos);
		if (now != was) {
			level.setBlock(pos, state.setValue(POWER, now), Block.UPDATE_ALL);
			level.updateNeighborsAt(pos, this);
		}
		if (was == 0 && now > 0) {
			level.playSound(null, pos, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.90000004F);
			level.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pos);
		} else if (was > 0 && now == 0) {
			level.playSound(null, pos, SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.75F);
			level.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, pos);
		}
		if (now > 0) {
			level.scheduleTick(pos, this, RECHECK_TICKS);
		}
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWER);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		boolean removed = !state.is(newState.getBlock());
		super.onRemove(state, level, pos, newState, moved);
		if (removed && state.getValue(POWER) > 0) {
			level.updateNeighborsAt(pos, this);
		}
	}

}
