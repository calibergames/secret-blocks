package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.jetbrains.annotations.Nullable;

/**
 * How the Builder's Goggles' view and the Secret Goggles' overlay draw their
 * quads through the Fabric Renderer API: each sets a quad's layer, light,
 * shading and ambient occlusion.
 */
@Environment(EnvType.CLIENT)
public final class FabricMaterials {

	/** A way of drawing a quad. */
	@FunctionalInterface
	public interface Material {
		void applyTo(MutableQuadView quad);
	}

	/** Translucent (no emissive light), for the faded disguise. */
	static final Material FADE = layer(ChunkSectionLayer.TRANSLUCENT);
	/** Solid and full-bright (no shading or ambient occlusion), for the outline. */
	static final Material OUTLINE = quad -> quad.chunkLayer(ChunkSectionLayer.SOLID).emissive(true).diffuseShade(false)
			.ambientOcclusion(TriState.FALSE);
	/** Translucent and full-bright (emissive, no shading or ambient occlusion), for the overlay. */
	static final Material OVERLAY = quad -> quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT).emissive(true).diffuseShade(false)
			.ambientOcclusion(TriState.FALSE);
	private static final Material REAL = layer(null);
	private static final Material REAL_FLAT = quad -> quad.emissive(false).diffuseShade(true)
			.ambientOcclusion(TriState.FALSE);

	private FabricMaterials() {
	}

	/** In {@code layer} (the quad's own, from its texture, for {@code null}), lit and shaded as usual. */
	static Material layer(@Nullable ChunkSectionLayer layer) {
		return quad -> {
			if (layer != null) {
				quad.chunkLayer(layer);
			}
			quad.emissive(false).diffuseShade(true).ambientOcclusion(TriState.DEFAULT);
		};
	}

	/** The Builder's Goggles' view can be drawn only with a Renderer API implementation. */
	public static boolean ensureBuilderView() {
		return renderer() != null;
	}

	/**
	 * For the real block drawn inside: the block's own pass, without ambient
	 * occlusion for a model that has none, as vanilla draws it.
	 */
	static Material real(boolean ambientOcclusion) {
		return ambientOcclusion ? REAL : REAL_FLAT;
	}

	/** The active Renderer API implementation, or null before one is registered. */
	static Renderer renderer() {
		try {
			return Renderer.get();
		} catch (UnsupportedOperationException noneYet) {
			return null;
		}
	}
}
