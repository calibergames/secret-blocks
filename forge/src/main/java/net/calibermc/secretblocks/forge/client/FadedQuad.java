package net.calibermc.secretblocks.forge.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * A disguise quad drawn faded (the Builder's Goggles view): its vertex
 * colours carry the alpha, and so does {@link #alpha()}. Forge's block
 * renderer uses the vertex colours' red, green and blue but not their alpha,
 * so it is applied for these quads only ({@code FadedQuadAlphaMixin}).
 */
public final class FadedQuad extends BakedQuad {

	private final float alpha;

	FadedQuad(int[] vertices, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion, int alpha) {
		super(vertices, -1, direction, sprite, shade, ambientOcclusion);
		this.alpha = alpha / 255.0f;
	}

	/** How opaque the quad is drawn, from 0 to 1. */
	public float alpha() {
		return alpha;
	}
}
