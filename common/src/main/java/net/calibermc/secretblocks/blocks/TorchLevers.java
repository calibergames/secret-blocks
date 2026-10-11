package net.calibermc.secretblocks.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * What the two Torch Lever blocks share ({@link TorchLever} on a floor,
 * {@link WallTorchLever} on a wall): the lever half. Toggled by use like
 * vanilla's {@code LeverBlock}: signal 15 on every side, strong power into
 * the block it is fixed to, the lever's click, and the same neighbour
 * updates. Switched on, the torch tilts like a thrown lever (the original
 * mod's look; our own models): its outline and its flame move with it.
 *
 * {@code away} is the direction from that block to the torch: up for the
 * floor torch, the wall torch's facing on a wall.
 */
final class TorchLevers {

	static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	private TorchLevers() {
	}

	static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, Direction away) {
		if (level.isClientSide) {
			// No redstone particles, unlike a lever: only the tilt gives it away.
			return InteractionResult.SUCCESS;
		}
		BlockState toggled = state.cycle(POWERED);
		level.setBlock(pos, toggled, 3);
		updateNeighbours(toggled, level, pos, away);
		boolean on = toggled.getValue(POWERED);
		level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, on ? 0.6F : 0.5F);
		level.gameEvent(player, on ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
		return InteractionResult.CONSUME;
	}

	/** The torch's neighbours and those of the block it is fixed to (that block is strongly powered). */
	static void updateNeighbours(BlockState state, Level level, BlockPos pos, Direction away) {
		level.updateNeighborsAt(pos, state.getBlock());
		level.updateNeighborsAt(pos.relative(away.getOpposite()), state.getBlock());
	}

	static int signal(BlockState state) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	/** Strong power only into the block it is fixed to: that block asks with the direction {@code away}. */
	static int directSignal(BlockState state, Direction direction, Direction away) {
		return state.getValue(POWERED) && direction == away ? 15 : 0;
	}

	/** The smoke and flame of a tilted (on) torch, at the tip of its model. */
	static void tiltedFlame(Level level, ParticleOptions flame, BlockPos pos, Vec3 offset) {
		double x = pos.getX() + offset.x, y = pos.getY() + offset.y, z = pos.getZ() + offset.z;
		level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
		level.addParticle(flame, x, y, z, 0.0, 0.0, 0.0);
	}

	/** Removed while on: the neighbours must hear that the signal is gone. */
	static void removed(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved, Direction away) {
		if (!moved && !state.is(newState.getBlock()) && state.getValue(POWERED)) {
			updateNeighbours(state, level, pos, away);
		}
	}
}
