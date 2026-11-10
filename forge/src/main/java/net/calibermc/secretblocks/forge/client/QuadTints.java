package net.calibermc.secretblocks.forge.client;

/**
 * Disguise quad colours on Forge. A quad carries no colour of its own: the
 * block renderer colours a quad only through its tint index and the block's
 * colour handler. A disguise quad's tint index holds its colour (red, green
 * and blue), marked so the secret blocks' colour handler ({@link #color})
 * reads it back, and marked again when the quad is drawn faded.
 */
public final class QuadTints {

	private static final int COLOURED = 0x4000_0000;
	private static final int FADED = 0x2000_0000;
	private static final int RGB = 0x00FF_FFFF;

	private QuadTints() {
	}

	/** The tint index for a quad drawn in {@code argb} (its alpha is applied apart, see {@link FadedQuad}). */
	static int tintIndex(int argb, boolean faded) {
		if (!faded && (argb & RGB) == RGB) {
			return -1;
		}
		return COLOURED | (faded ? FADED : 0) | (argb & RGB);
	}

	/** The colour a tint index holds: white for any other tint index. */
	public static int color(int tintIndex) {
		return tintIndex != -1 && (tintIndex & COLOURED) != 0 ? 0xFF00_0000 | (tintIndex & RGB) : 0xFFFF_FFFF;
	}

	/** Whether a quad with this tint index may be a faded disguise quad. */
	static boolean marksFaded(int tintIndex) {
		return tintIndex != -1 && (tintIndex & (COLOURED | FADED)) == (COLOURED | FADED);
	}
}
