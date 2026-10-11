package net.calibermc.secretblocks.render;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Plain maths for building disguise face quads by hand, where a loader has no
 * Fabric Renderer API: a face rectangle's corners as that API's
 * {@code QuadEmitter.square} lays them out, texture coordinates locked to the
 * corners' positions as its locked-texture baking does, and vertex colours.
 */
public final class FaceQuads {

	/** How close to the block's side a face counts as lying on it (and is culled with it). */
	public static final float CULL_EPSILON = 1.0e-5f;

	private FaceQuads() {
	}

	/**
	 * A face rectangle: its four corners (x, y, z in the block) and the side it
	 * is culled with, or null if it lies inside the block.
	 */
	public record Corners(float[][] positions, @Nullable Direction cullFace) {
	}

	/**
	 * The corners of a face rectangle given by its left, bottom, right and top
	 * edges seen from outside and its depth in from the block's side, in the
	 * order {@code QuadEmitter.square} gives them (counter-clockwise seen from
	 * outside).
	 */
	public static Corners square(Direction face, float left, float bottom, float right, float top, float depth) {
		Direction cullFace = null;
		if (Math.abs(depth) < CULL_EPSILON) {
			cullFace = face;
			depth = 0;
		}
		float[][] p = new float[4][];
		switch (face) {
			case UP, DOWN -> {
				if (face == Direction.UP) {
					depth = 1 - depth;
					top = 1 - top;
					bottom = 1 - bottom;
				}
				p[0] = new float[] {left, depth, top};
				p[1] = new float[] {left, depth, bottom};
				p[2] = new float[] {right, depth, bottom};
				p[3] = new float[] {right, depth, top};
			}
			case EAST, WEST -> {
				if (face == Direction.EAST) {
					depth = 1 - depth;
					left = 1 - left;
					right = 1 - right;
				}
				p[0] = new float[] {depth, top, left};
				p[1] = new float[] {depth, bottom, left};
				p[2] = new float[] {depth, bottom, right};
				p[3] = new float[] {depth, top, right};
			}
			case NORTH, SOUTH -> {
				if (face == Direction.SOUTH) {
					depth = 1 - depth;
					left = 1 - left;
					right = 1 - right;
				}
				p[0] = new float[] {1 - left, top, depth};
				p[1] = new float[] {1 - left, bottom, depth};
				p[2] = new float[] {1 - right, bottom, depth};
				p[3] = new float[] {1 - right, top, depth};
			}
		}
		return new Corners(p, cullFace);
	}

	/** Texture coordinates (0 to 1) locked to a corner's position on a face, as vanilla's block faces lie. */
	public static float[] lockedUv(Direction face, float[] corner) {
		return switch (face) {
			case EAST -> new float[] {1 - corner[2], 1 - corner[1]};
			case WEST -> new float[] {corner[2], 1 - corner[1]};
			case NORTH -> new float[] {1 - corner[0], 1 - corner[1]};
			case SOUTH -> new float[] {corner[0], 1 - corner[1]};
			case DOWN -> new float[] {corner[0], 1 - corner[2]};
			case UP -> new float[] {corner[0], corner[2]};
		};
	}

	/** Two ARGB colours multiplied channel by channel. */
	public static int multiply(int a, int b) {
		int result = 0;
		for (int shift = 0; shift < 32; shift += 8) {
			int channel = ((a >>> shift) & 0xFF) * ((b >>> shift) & 0xFF) / 255;
			result |= channel << shift;
		}
		return result;
	}

	/**
	 * Swaps the red and blue bytes: ARGB to the block vertex format's colour
	 * (RGBA bytes read as a little-endian int), and back.
	 */
	public static int swapRedBlue(int color) {
		return (color & 0xFF00_FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16);
	}
}
