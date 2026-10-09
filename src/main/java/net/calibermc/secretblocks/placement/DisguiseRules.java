package net.calibermc.secretblocks.placement;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Picks the disguise for a newly placed secret block.
 *
 * Pure: no Minecraft types, so it runs under plain JUnit and can move to the
 * loader-neutral {@code common} module unchanged. {@code S} is the block-state
 * type; the Minecraft adapter is {@link DisguisePlacement}.
 *
 * The rules:
 * <ul>
 *   <li>The disguise comes from the <b>neighbor the new block was placed
 *   against</b> and the face of that neighbor that touches the new block.</li>
 *   <li>Neighbor is a secret block → copy that neighbor's disguise for the
 *   touching face (P6), for any secret block type.</li>
 *   <li>Neighbor is a full cube → copy its state.</li>
 *   <li>Anything else (air, slabs, fences, panes…) → stone (P7).</li>
 *   <li>One Way Glass → glass on the face toward the viewer; see
 *   {@link #glassFace}.</li>
 * </ul>
 * Which neighbor applies when the click replaced a block (P2), and that
 * stacking into an existing block keeps its disguise (P10), are decided by the
 * adapter; this class only sees the chosen neighbor.
 */
public final class DisguiseRules {

	private DisguiseRules() {
	}

	/** The six faces of a block. Mirrors Minecraft's {@code Direction} without depending on it. */
	public enum Face {
		DOWN, UP, NORTH, SOUTH, WEST, EAST;

		public Face opposite() {
			return switch (this) {
				case DOWN -> UP;
				case UP -> DOWN;
				case NORTH -> SOUTH;
				case SOUTH -> NORTH;
				case WEST -> EAST;
				case EAST -> WEST;
			};
		}
	}

	/** What the new block was placed against. */
	public sealed interface Neighbor<S> permits SecretNeighbor, FullCube, Other {
	}

	/** Another secret block, with its current disguise per face. */
	public record SecretNeighbor<S>(Map<Face, S> faces) implements Neighbor<S> {
		public SecretNeighbor {
			Objects.requireNonNull(faces);
		}
	}

	/** A block whose collision shape is a full cube; its state is copied. */
	public record FullCube<S>(S state) implements Neighbor<S> {
		public FullCube {
			Objects.requireNonNull(state);
		}
	}

	/** Air, a partial block, or anything else that cannot be copied. */
	public record Other<S>() implements Neighbor<S> {
	}

	/**
	 * The disguise for all six faces.
	 *
	 * @param neighbor      what the block was placed against
	 * @param touchingFace  the neighbor's face that touches the new block
	 *                      (for a normal click, the clicked face)
	 * @param glassFace     for One Way Glass, the face that becomes glass;
	 *                      {@code null} for every other block
	 * @param stone         the fallback state
	 * @param glass         the glass state
	 */
	public static <S> Map<Face, S> select(Neighbor<S> neighbor, Face touchingFace, Face glassFace, S stone, S glass) {
		Objects.requireNonNull(touchingFace);
		S base;
		if (neighbor instanceof SecretNeighbor<S> secret) {
			S face = secret.faces().get(touchingFace);
			base = face != null ? face : stone;
		} else if (neighbor instanceof FullCube<S> cube) {
			base = cube.state();
		} else {
			base = stone;
		}
		Map<Face, S> result = new EnumMap<>(Face.class);
		for (Face face : Face.values()) {
			result.put(face, face == glassFace ? glass : base);
		}
		return result;
	}

	/**
	 * The One Way Glass face: the face toward the placing player, opposite
	 * the way they face. That is their horizontal facing, as in 1.2.0 (P4),
	 * or, when they sneak, the nearest direction of their look, so the glass
	 * can go on the top or bottom.
	 * With no player (automation, P3b) the glass goes on the face pointing
	 * away from the clicked block, which is the face a player clicking that
	 * block head-on would get.
	 *
	 * @param playerFacing  the way the player faces, or {@code null} if
	 *                      no player placed the block
	 * @param touchingFace  the neighbor's face that touches the new block
	 */
	public static Face glassFace(Face playerFacing, Face touchingFace) {
		if (playerFacing != null) {
			return playerFacing.opposite();
		}
		return touchingFace;
	}
}
