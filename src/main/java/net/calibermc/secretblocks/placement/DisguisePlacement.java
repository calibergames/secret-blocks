package net.calibermc.secretblocks.placement;

import net.calibermc.secretblocks.blocks.OneWayGlassBlock;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.placement.DisguiseRules.Face;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.EnumMap;
import java.util.Map;

/**
 * Minecraft adapter for {@link DisguiseRules}: reads the placement context,
 * picks the disguise on the server, and writes it to the new block entity.
 * Used by {@link net.calibermc.secretblocks.items.SecretBlockItem}; the
 * vanilla block-entity sync carries the result to clients.
 */
public final class DisguisePlacement {

	private DisguisePlacement() {
	}

	/**
	 * The disguise for the block {@code placing} is about to put down, or
	 * {@code null} to leave the block's current disguise alone (stacking a slab
	 * into an existing slab of the same block, P10). Call before vanilla
	 * placement changes the world.
	 */
	public static Map<Direction, BlockState> choose(BlockPlaceContext context, Block placing) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.getBlockState(pos).is(placing)) {
			return null; // merging into the same block (P10): keep its disguise
		}
		BlockPos neighborPos;
		Direction touching;
		if (context.replacingClickedOnBlock()) {
			// The click replaced a block such as tall grass or a snow layer (P2):
			// copy the block it stood on.
			neighborPos = pos.below();
			touching = Direction.UP;
		} else {
			touching = context.getClickedFace();
			neighborPos = pos.relative(touching.getOpposite());
		}
		Face glassFace = null;
		if (placing instanceof OneWayGlassBlock) {
			Face playerFacing = context.getPlayer() == null ? null : face(context.getPlayer().getDirection());
			glassFace = DisguiseRules.glassFace(playerFacing, face(touching));
		}
		Map<Face, BlockState> faces = DisguiseRules.select(neighbor(level, neighborPos), face(touching), glassFace,
				Blocks.STONE.defaultBlockState(), Blocks.GLASS.defaultBlockState());
		Map<Direction, BlockState> result = new EnumMap<>(Direction.class);
		faces.forEach((face, state) -> result.put(direction(face), state));
		return result;
	}

	/** Write {@code disguise} to the secret block at {@code pos} (and a door's upper half). Server only. */
	public static void apply(Level level, BlockPos pos, Map<Direction, BlockState> disguise) {
		if (level.isClientSide()) {
			return;
		}
		write(level, pos, disguise);
		BlockState state = level.getBlockState(pos);
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
				&& state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
			write(level, pos.above(), disguise);
		}
	}

	private static void write(Level level, BlockPos pos, Map<Direction, BlockState> disguise) {
		if (level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret) {
			secret.setDisguise(disguise);
		}
	}

	static DisguiseRules.Neighbor<BlockState> neighbor(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof SecretBlock && level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret) {
			Map<Face, BlockState> faces = new EnumMap<>(Face.class);
			for (Direction dir : Direction.values()) {
				faces.put(face(dir), secret.getState(dir));
			}
			return new DisguiseRules.SecretNeighbor<>(faces);
		}
		if (!state.isAir() && state.isCollisionShapeFullBlock(level, pos)) {
			return new DisguiseRules.FullCube<>(state);
		}
		return new DisguiseRules.Other<>();
	}

	static Face face(Direction dir) {
		return switch (dir) {
			case DOWN -> Face.DOWN;
			case UP -> Face.UP;
			case NORTH -> Face.NORTH;
			case SOUTH -> Face.SOUTH;
			case WEST -> Face.WEST;
			case EAST -> Face.EAST;
		};
	}

	static Direction direction(Face face) {
		return switch (face) {
			case DOWN -> Direction.DOWN;
			case UP -> Direction.UP;
			case NORTH -> Direction.NORTH;
			case SOUTH -> Direction.SOUTH;
			case WEST -> Direction.WEST;
			case EAST -> Direction.EAST;
		};
	}
}
