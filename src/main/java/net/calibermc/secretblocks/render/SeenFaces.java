package net.calibermc.secretblocks.render;

/**
 * Which faces of a block a camera can see: the faces whose plane the camera
 * is beyond (for the Builder's Goggles' chest lighting). Pure
 * logic with no Minecraft types, so JUnit tests it ({@code SeenFacesTest}).
 * One bit per face, in Minecraft's direction order (down, up, north, south,
 * west, east), so {@code 1 << direction.get3DDataValue()} is a face's bit.
 */
public final class SeenFaces {

	public static final int DOWN = 1;
	public static final int UP = 1 << 1;
	public static final int NORTH = 1 << 2;
	public static final int SOUTH = 1 << 3;
	public static final int WEST = 1 << 4;
	public static final int EAST = 1 << 5;
	public static final int ALL = DOWN | UP | NORTH | SOUTH | WEST | EAST;

	private SeenFaces() {
	}

	/**
	 * The faces of the block at {@code (x, y, z)} seen from the camera at
	 * {@code (cameraX, cameraY, cameraZ)}. None (0) when the camera is inside
	 * the block; a camera exactly on a face's plane does not see that face.
	 */
	public static int of(int x, int y, int z, double cameraX, double cameraY, double cameraZ) {
		int seen = 0;
		if (cameraY < y) {
			seen |= DOWN;
		}
		if (cameraY > y + 1) {
			seen |= UP;
		}
		if (cameraZ < z) {
			seen |= NORTH;
		}
		if (cameraZ > z + 1) {
			seen |= SOUTH;
		}
		if (cameraX < x) {
			seen |= WEST;
		}
		if (cameraX > x + 1) {
			seen |= EAST;
		}
		return seen;
	}
}
