package net.calibermc.secretblocks.forge.client;

import net.minecraft.client.resources.model.geometry.BakedQuad;

/**
 * Disguise quad colours on Forge. A quad carries no colour of its own: the
 * block renderer colours a quad only through its tint index and the block's
 * tint sources. A disguise quad's tint index holds its colour (red, green and
 * blue), marked so the block renderer ({@code DisguiseQuadColorMixin}) applies
 * it ({@link #argb}), and marked again when the quad is drawn faded.
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

	/** Whether a quad's tint index holds its colour. */
	public static boolean holdsColor(int tintIndex) {
		return tintIndex != -1 && (tintIndex & COLOURED) != 0;
	}

	/** The colour a disguise quad is drawn in, its alpha below 255 only when it is drawn faded. */
	public static int argb(BakedQuad quad) {
		int tintIndex = quad.materialInfo().tintIndex();
		int alpha = Math.round(FadedQuad.alpha(quad) * 255.0f);
		return alpha << 24 | (holdsColor(tintIndex) ? tintIndex & RGB : RGB);
	}

	/** Whether a quad with this tint index may be a faded disguise quad. */
	static boolean marksFaded(int tintIndex) {
		return tintIndex != -1 && (tintIndex & (COLOURED | FADED)) == (COLOURED | FADED);
	}
}
