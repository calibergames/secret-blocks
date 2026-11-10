package net.calibermc.secretblocks.forge.client;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.renderer.block.model.BakedQuad;

/**
 * The disguise quads drawn faded (the Builder's Goggles view): their vertex
 * colours carry the alpha. Forge's block renderer uses the vertex colours'
 * red, green and blue but not their alpha, so it is applied for these quads
 * only ({@code FadedQuadAlphaMixin}). They are known by identity, so no other
 * quad, however it is coloured, is drawn differently.
 */
public final class FadedQuad {

	/** Where the first vertex's colour sits in a quad's vertex data. */
	private static final int COLOR = 3;

	private static final Map<BakedQuad, Float> FADED = Collections.synchronizedMap(new WeakHashMap<>());

	private FadedQuad() {
	}

	/** {@code quad}, drawn with the alpha its vertex colours carry ({@code alpha}, 0 to 255). */
	static BakedQuad faded(BakedQuad quad, int alpha) {
		FADED.put(quad, alpha / 255.0f);
		return quad;
	}

	/** How opaque {@code quad} is drawn, from 0 to 1: below 1 only for a faded disguise quad. */
	public static float alpha(BakedQuad quad) {
		// Only a quad whose vertex colours are not opaque can be one of them; most are.
		if (quad.vertices()[COLOR] >>> 24 == 0xFF) {
			return 1.0f;
		}
		Float alpha = FADED.get(quad);
		return alpha != null ? alpha : 1.0f;
	}
}
