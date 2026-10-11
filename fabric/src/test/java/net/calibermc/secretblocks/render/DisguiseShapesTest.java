package net.calibermc.secretblocks.render;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** A face's texture turns with its disguise's rotation as it always has. */
class DisguiseShapesTest {

	/**
	 * The corner each corner takes its texture from, per rotation, as the
	 * renderer turned textures before the order became a table: the top face
	 * first, then every other face.
	 */
	private static final int[][][] EARLIER = {
			{{3, 2, 1, 0}, {0, 1, 2, 3}}, // 0
			{{0, 3, 2, 1}, {1, 2, 3, 0}}, // 90
			{{1, 0, 3, 2}, {2, 3, 0, 1}}, // 180
			{{2, 1, 0, 3}, {3, 0, 1, 2}}, // 270
	};

	@Test
	void uvOrderMatchesTheEarlierTurns() {
		int[] rotations = {0, 90, 180, 270};
		for (int r = 0; r < rotations.length; r++) {
			for (Direction face : Direction.values()) {
				int[] expected = EARLIER[r][face == Direction.UP ? 0 : 1];
				assertArrayEquals(expected, DisguiseShapes.uvOrder(rotations[r], face), "rotation " + rotations[r] + ", " + face);
			}
		}
	}

	@Test
	void anyOtherRotationLeavesTheTextureAsItIs() {
		for (Direction face : Direction.values()) {
			assertArrayEquals(new int[] {0, 1, 2, 3}, DisguiseShapes.uvOrder(45, face), face.toString());
			assertArrayEquals(new int[] {0, 1, 2, 3}, DisguiseShapes.uvOrder(-1, face), face.toString());
		}
	}
}
