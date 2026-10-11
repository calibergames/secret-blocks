package net.calibermc.secretblocks.blocks;

import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Secret Daylight Detector (the original mod's design, our own code): a
 * daylight detector in a full disguised block.
 *
 * Vanilla's daylight code is private and reads the sky light at the
 * detector's own position, which is 0 inside a full, opaque block. So this is
 * its own block with vanilla's formula ({@link #signalStrength}) applied to
 * the brightest sky light of its six neighbouring spaces: it works when
 * any face is open to the sky, and a buried one reads as in the dark.
 * Everything else follows {@code DaylightDetectorBlock}: vanilla's own
 * {@code POWER} and {@code INVERTED} properties, use toggling inverted (only
 * for a player who may build), an update once a second on the same game tick
 * as vanilla's, and no updates in a dimension without sky light. It gives
 * weak power on every side; not a redstone conductor (set in
 * {@code ModBlocks}).
 */
public class SecretDaylightDetector extends Block implements EntityBlock, SecretBlock {

	/** Vanilla's own property objects, so a state converts to and from a vanilla detector's. */
	public static final IntegerProperty POWER = DaylightDetectorBlock.POWER;
	public static final BooleanProperty INVERTED = DaylightDetectorBlock.INVERTED;

	public SecretDaylightDetector(Properties settings) {
		super(settings);
		registerDefaultState(stateDefinition.any().setValue(POWER, 0).setValue(INVERTED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWER, INVERTED);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretBlockEntity(pos, state);
	}

	/** As vanilla: once a second, on the server, and only where there is a sky. */
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || !level.dimensionType().hasSkyLight() || type != ModEntities.SECRET_BLOCK_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> {
			if (tickLevel.getGameTime() % 20L == 0L) {
				updateSignalStrength(tickState, tickLevel, pos);
			}
		};
	}

	/** The brightest sky light among the six spaces around the block. */
	public static int skyLightAround(Level level, BlockPos pos) {
		int brightest = 0;
		for (Direction direction : Direction.values()) {
			brightest = Math.max(brightest, level.getBrightness(LightLayer.SKY, pos.relative(direction)));
		}
		return brightest;
	}

	/**
	 * Vanilla's formula ({@code DaylightDetectorBlock.updateSignalStrength}) for a
	 * given sky light: the light minus the sky's darkening, then either inverted
	 * or scaled by the sun's angle; 0 to 15.
	 */
	public static int signalStrength(int skyLight, int skyDarken, float sunAngle, boolean inverted) {
		int strength = skyLight - skyDarken;
		if (inverted) {
			strength = 15 - strength;
		} else if (strength > 0) {
			float towards = sunAngle < (float) Math.PI ? 0.0F : (float) (Math.PI * 2);
			float angle = sunAngle + (towards - sunAngle) * 0.2F;
			strength = Math.round(strength * Mth.cos(angle));
		}
		return Mth.clamp(strength, 0, 15);
	}

	private static void updateSignalStrength(BlockState state, Level level, BlockPos pos) {
		int strength = signalStrength(skyLightAround(level, pos), level.getSkyDarken(), level.getSunAngle(1.0F), state.getValue(INVERTED));
		if (state.getValue(POWER) != strength) {
			level.setBlock(pos, state.setValue(POWER, strength), 3);
		}
	}

	/** As vanilla: a player who may build toggles inverted, and the signal is recomputed at once. */
	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!player.mayBuild()) {
			return super.use(state, level, pos, player, hand, hit);
		}
		if (level.isClientSide) {
			return InteractionResult.SUCCESS;
		}
		BlockState toggled = state.cycle(INVERTED);
		level.setBlock(pos, toggled, 4);
		level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, toggled));
		updateSignalStrength(toggled, level, pos);
		return InteractionResult.CONSUME;
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
}
