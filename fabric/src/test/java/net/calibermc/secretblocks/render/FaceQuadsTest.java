package net.calibermc.secretblocks.render;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Face rectangles and their textures land where a block's own faces do. */
class FaceQuadsTest {

	private static final float EPS = 1.0e-6f;

	/** The coordinate along the face's normal (x, y or z). */
	private static int axis(Direction face) {
		return switch (face.getAxis()) {
			case X -> 0;
			case Y -> 1;
			case Z -> 2;
		};
	}

	@Test
	void aFaceOnTheBlocksSideLiesOnItAndIsCulledWithIt() {
		for (Direction face : Direction.values()) {
			FaceQuads.Corners corners = FaceQuads.square(face, 0, 0, 1, 1, 0);
			float side = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
			for (float[] corner : corners.positions()) {
				assertEquals(side, corner[axis(face)], EPS, face + " corner off the block's side");
			}
			assertEquals(face, corners.cullFace());
		}
	}

	@Test
	void aFaceInsideTheBlockIsNotCulled() {
		for (Direction face : Direction.values()) {
			FaceQuads.Corners corners = FaceQuads.square(face, 0, 0, 1, 1, 0.25f);
			float plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0.75f : 0.25f;
			for (float[] corner : corners.positions()) {
				assertEquals(plane, corner[axis(face)], EPS, face + " corner off its plane");
			}
			assertNull(corners.cullFace(), face.toString());
		}
	}

	@Test
	void cornersWindToFaceOutward() {
		for (Direction face : Direction.values()) {
			float[][] p = FaceQuads.square(face, 0, 0, 1, 1, 0).positions();
			float[] a = {p[1][0] - p[0][0], p[1][1] - p[0][1], p[1][2] - p[0][2]};
			float[] b = {p[2][0] - p[1][0], p[2][1] - p[1][1], p[2][2] - p[1][2]};
			float[] n = {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
			assertEquals(face.getStepX(), (int) Math.signum(n[0]), face + " normal x");
			assertEquals(face.getStepY(), (int) Math.signum(n[1]), face + " normal y");
			assertEquals(face.getStepZ(), (int) Math.signum(n[2]), face + " normal z");
		}
	}

	@Test
	void leftAndRightAreAsSeenFromOutside() {
		// The north face seen from the north: east (+x) is on the left.
		float[][] north = FaceQuads.square(Direction.NORTH, 0, 0, 0.25f, 1, 0).positions();
		for (float[] corner : north) {
			assertTrue(corner[0] >= 0.75f - EPS, "a strip at the north face's left edge should be at its east end");
		}
		// The south face seen from the south: west (-x) is on the left.
		float[][] south = FaceQuads.square(Direction.SOUTH, 0, 0, 0.25f, 1, 0).positions();
		for (float[] corner : south) {
			assertTrue(corner[0] <= 0.25f + EPS, "a strip at the south face's left edge should be at its west end");
		}
	}

	@Test
	void aWholeFacesTextureIsUpright() {
		float[][] expected = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
		for (Direction face : Direction.values()) {
			float[][] p = FaceQuads.square(face, 0, 0, 1, 1, 0).positions();
			for (int i = 0; i < 4; i++) {
				assertArrayEquals(expected[i], FaceQuads.lockedUv(face, p[i]), EPS, face + " corner " + i);
			}
		}
	}

	@Test
	void coloursPackAndMultiply() {
		assertEquals(0x80332211, FaceQuads.swapRedBlue(0x80112233));
		assertEquals(0x80112233, FaceQuads.swapRedBlue(FaceQuads.swapRedBlue(0x80112233)));
		assertEquals(0x80112233, FaceQuads.multiply(0x80112233, 0xFFFFFFFF));
		assertEquals(0x00000000, FaceQuads.multiply(0x80112233, 0x00000000));
		assertEquals(0x7F7F7F7F, FaceQuads.multiply(0xFFFFFFFF, 0x7F7F7F7F));
	}
}
