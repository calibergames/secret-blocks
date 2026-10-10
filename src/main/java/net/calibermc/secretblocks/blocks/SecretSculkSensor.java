package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretSculkSensorBlockEntity;
import net.calibermc.secretblocks.registry.ModEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Secret Sculk Sensor: a vanilla sculk sensor in a full disguised block.
 * Vanilla's code is kept whole: it hears vibrations within 8 blocks (wool
 * blocks them), gives a signal by distance and the vibration's frequency to a
 * comparator, stays active 40 ticks with a one-tick cooldown, is set off by
 * stepping on it, and clicks. Vanilla asks for
 * "a {@code SculkSensorBlockEntity}", so the block entity is a subclass of it
 * with the disguise added ({@link SecretSculkSensorBlockEntity}).
 *
 * Differences from vanilla: a full block; never holds water (water could not
 * show in a full block); breaking it drops itself and no experience; no
 * particles from its sides while active, so it does not give itself away (as
 * the Secret Repeater). Not a redstone conductor (set in {@code ModBlocks}).
 */
public class SecretSculkSensor extends SculkSensorBlock implements SecretBlock {

	public SecretSculkSensor(Properties settings) {
		super(settings);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretSculkSensorBlockEntity(pos, state);
	}

	/** Vanilla's ticker only accepts the vanilla type; run the same tick (the vibration in flight) for ours. */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModEntities.SECRET_SCULK_SENSOR,
				(tickLevel, pos, tickState, sensor) -> VibrationSystem.Ticker.tick(tickLevel, sensor.getVibrationData(), sensor.getVibrationUser()));
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

	/** A full block blocks light as the other secret blocks do (vanilla's half block uses its shape). */
	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return false;
	}

	/**
	 * Never holds water. Vanilla's code reads the {@code waterlogged}
	 * property, so it stays, but placing never sets it, a bucket cannot fill
	 * it, and a state that has it anyway (a command, a structure, a vanilla
	 * sensor turned secret) holds no water and drops it at the next update.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(WATERLOGGED, false);
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return Fluids.EMPTY.defaultFluidState();
	}

	/** Vanilla's, without the water tick it schedules for a waterlogged sensor. */
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return state.getValue(WATERLOGGED) ? state.setValue(WATERLOGGED, false) : state;
	}

	@Override
	public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
		return false;
	}

	@Override
	public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
		return false;
	}

	/** Breaking it gives no experience (vanilla gives 5 without Silk Touch). It drops itself (loot table). */
	@Override
	public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
	}

	/** No particles from its sides while active: they would show where it hides. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
	}
}
