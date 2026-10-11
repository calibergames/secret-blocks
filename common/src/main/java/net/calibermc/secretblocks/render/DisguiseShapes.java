package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.DoorBlock;
import net.calibermc.secretblocks.blocks.TrapdoorBlock;
import net.calibermc.secretblocks.registry.Counterparts;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.Optional;

/**
 * The geometry of a disguise, shared by each loader's renderer: the shape of
 * each face (a full square, or a door or trapdoor panel's face), how a face's
 * texture turns with its rotation, and the vanilla block drawn inside in the
 * Builder's Goggles' view.
 */
public final class DisguiseShapes {

	private DisguiseShapes() {
	}

	/**
	 * One face rectangle, as Fabric's {@code QuadEmitter.square} takes it: the
	 * face, its left, bottom, right and top edges seen from outside, and its
	 * depth in from the block's side.
	 */
	@FunctionalInterface
	public interface Square {
		void at(Direction face, float left, float bottom, float right, float top, float depth);
	}

	/** The shape of one disguise face: a full square, or a door or trapdoor panel face. */
	public static void shape(BlockState state, Direction direction, Square square) {
		if (state.getBlock() instanceof DoorBlock) {
			renderDoor(square, state, direction);
		} else if (state.getBlock() instanceof TrapdoorBlock) {
			renderTrapdoor(square, state, direction);
		} else {
			square.at(direction, 0.0f, 0.0f, 1.0f, 1.0f, 0);
		}
	}

	private static void renderDoorCuboid(Square square, Direction faceDirection, Direction cuboidDirection) {
		switch (cuboidDirection) {

		case NORTH:

			if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0f);
			} else if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			}
			break;

		case EAST:

			if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			}
			break;

		case SOUTH:

			if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0f);
			} else if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			}
			break;

		case WEST:

			if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.8125f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.0f, 0.1875f, 1.0f, 0.0f);
			}
			break;

		case UP:

			if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.8125f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			}
			break;

		case DOWN:

			if (faceDirection == Direction.SOUTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			} else if (faceDirection == Direction.NORTH) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			} else if (faceDirection == Direction.WEST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			} else if (faceDirection == Direction.EAST) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 0.1875f, 0.0f);
			} else if (faceDirection == Direction.DOWN) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f);
			} else if (faceDirection == Direction.UP) {
				square.at(faceDirection, 0.0f, 0.0f, 1.0f, 1.0f, 0.8125f);
			}
			break;
		}
	}

	private static void renderDoor(Square square, BlockState state, Direction direction) {

		Direction renderDirection = Direction.EAST;

		if ((state.getValue(DoorBlock.FACING) == Direction.NORTH && !state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.EAST && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT && state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.WEST && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT && state.getValue(DoorBlock.OPEN))) {
			renderDirection = Direction.SOUTH;
		} else if ((state.getValue(DoorBlock.FACING) == Direction.EAST && !state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.SOUTH && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT && state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.NORTH && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT && state.getValue(DoorBlock.OPEN))) {
			renderDirection = Direction.WEST;
		} else if ((state.getValue(DoorBlock.FACING) == Direction.SOUTH && !state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.WEST && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT && state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.EAST && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT && state.getValue(DoorBlock.OPEN))) {
			renderDirection = Direction.NORTH;
		} else if ((state.getValue(DoorBlock.FACING) == Direction.WEST && !state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.NORTH && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT && state.getValue(DoorBlock.OPEN)) || (state.getValue(DoorBlock.FACING) == Direction.SOUTH && state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT && state.getValue(DoorBlock.OPEN))) {
			renderDirection = Direction.EAST;
		} else {
			renderDirection = Direction.NORTH;
		}
		renderDoorCuboid(square, direction, renderDirection);
	}

	private static void renderTrapdoor(Square square, BlockState state, Direction direction) {
		if (state.getValue(TrapdoorBlock.OPEN)) {
			renderDoorCuboid(square, direction, state.getValue(TrapdoorBlock.FACING).getOpposite());
		} else {
			if (state.getValue(TrapdoorBlock.HALF) == Half.TOP) {
				renderDoorCuboid(square, direction, Direction.UP);
			} else if (state.getValue(TrapdoorBlock.HALF) == Half.BOTTOM) {
				renderDoorCuboid(square, direction, Direction.DOWN);
			}
		}
	}

	/**
	 * The order of a face's texture corners after its rotation: corner
	 * {@code i} takes the texture coordinates of corner {@code order[i]}
	 * (corners as {@code Square} lays them out). The top face turns the other
	 * way round, as the textures were drawn.
	 */
	public static int[] uvOrder(int rotation, Direction dir) {
		boolean up = dir == Direction.UP;
		return switch (rotation) {
			case 0 -> up ? new int[] {3, 2, 1, 0} : new int[] {0, 1, 2, 3};
			case 90 -> up ? new int[] {0, 3, 2, 1} : new int[] {1, 2, 3, 0};
			case 180 -> up ? new int[] {1, 0, 3, 2} : new int[] {2, 3, 0, 1};
			case 270 -> up ? new int[] {2, 1, 0, 3} : new int[] {3, 0, 1, 2};
			default -> new int[] {0, 1, 2, 3};
		};
	}

	/**
	 * The vanilla block drawn inside: the counterpart, and for the Secret
	 * Piston's head (which has none: the wand never converts it) vanilla's
	 * piston head, so an extended piston shows its head.
	 */
	public static Optional<BlockState> realStateOf(BlockState state) {
		if (state.is(ModBlocks.SECRET_PISTON_HEAD)) {
			return Optional.of(Counterparts.copyShared(state, Blocks.PISTON_HEAD.defaultBlockState()));
		}
		return Counterparts.vanillaStateOf(state);
	}
}
