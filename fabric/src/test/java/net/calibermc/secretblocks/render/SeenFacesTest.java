package net.calibermc.secretblocks.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Which faces of a block the camera sees (chest lighting under the Builder's Goggles). */
class SeenFacesTest {

	@Test
	void eachSideAlone() {
		assertEquals(SeenFaces.DOWN, SeenFaces.of(0, 0, 0, 0.5, -2, 0.5));
		assertEquals(SeenFaces.UP, SeenFaces.of(0, 0, 0, 0.5, 3, 0.5));
		assertEquals(SeenFaces.NORTH, SeenFaces.of(0, 0, 0, 0.5, 0.5, -2));
		assertEquals(SeenFaces.SOUTH, SeenFaces.of(0, 0, 0, 0.5, 0.5, 3));
		assertEquals(SeenFaces.WEST, SeenFaces.of(0, 0, 0, -2, 0.5, 0.5));
		assertEquals(SeenFaces.EAST, SeenFaces.of(0, 0, 0, 3, 0.5, 0.5));
	}

	@Test
	void cornersSeeThreeFaces() {
		assertEquals(SeenFaces.UP | SeenFaces.SOUTH | SeenFaces.EAST, SeenFaces.of(10, 64, -5, 12, 66, -3));
		assertEquals(SeenFaces.DOWN | SeenFaces.NORTH | SeenFaces.WEST, SeenFaces.of(10, 64, -5, 9.5, 63.5, -5.5));
	}

	@Test
	void insideSeesNothing() {
		assertEquals(0, SeenFaces.of(3, -60, 22, 3.5, -59.5, 22.5));
	}

	@Test
	void onAPlaneDoesNotSeeThatFace() {
		assertEquals(0, SeenFaces.of(0, 0, 0, 1, 0.5, 0.5));
		assertEquals(0, SeenFaces.of(0, 0, 0, 0.5, 0, 0.5));
		assertEquals(0, SeenFaces.of(0, 0, 0, 0.5, 1, 0.5));
		assertEquals(0, SeenFaces.of(0, 0, 0, 0, 0.5, 0.5));
		assertEquals(0, SeenFaces.of(0, 0, 0, 0.5, 0.5, 0));
		assertEquals(0, SeenFaces.of(0, 0, 0, 0.5, 0.5, 1));
		assertEquals(SeenFaces.SOUTH, SeenFaces.of(0, 0, 0, 0, 0.5, 1.5));
	}

	/** A chest set in a room's north wall, seen from inside the room: only the room side (as in the client check). */
	@Test
	void wallChestSeenFromTheRoom() {
		assertEquals(SeenFaces.SOUTH | SeenFaces.EAST, SeenFaces.of(153, -58, 22, 154.5, -57.9, 28.5));
		assertEquals(SeenFaces.SOUTH, SeenFaces.of(153, -58, 22, 153.5, -57.5, 28.5));
	}
}
