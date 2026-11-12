package net.calibermc.secretblocks.forge.client;

import java.util.Map;

import com.google.common.collect.MapMaker;

import net.minecraft.client.resources.model.geometry.BakedQuad;

/**
 * The disguise quads drawn faded (the Builder's Goggles view). Forge's block
 * renderer draws every quad opaque, so their alpha is applied for these quads
 * only ({@code DisguiseQuadColorMixin}). They are known by identity, so no other
 * quad is drawn differently; their tint index marks them ({@link QuadTints}),
 * so other quads are passed over at once.
 */
public final class FadedQuad {

	/** By identity, weakly: a quad stays marked while it is drawn and no longer. */
	private static final Map<BakedQuad, Float> FADED = new MapMaker().weakKeys().makeMap();

	private FadedQuad() {
	}

	/** {@code quad}, drawn with {@code alpha} (0 to 255). */
	static BakedQuad faded(BakedQuad quad, int alpha) {
		FADED.put(quad, alpha / 255.0f);
		return quad;
	}

	/** How opaque {@code quad} is drawn, from 0 to 1: below 1 only for a faded disguise quad. */
	public static float alpha(BakedQuad quad) {
		if (!QuadTints.marksFaded(quad.materialInfo().tintIndex())) {
			return 1.0f;
		}
		Float alpha = FADED.get(quad);
		return alpha != null ? alpha : 1.0f;
	}
}
