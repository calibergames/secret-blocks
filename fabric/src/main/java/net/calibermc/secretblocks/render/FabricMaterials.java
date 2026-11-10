package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.util.TriState;

/**
 * The Fabric Renderer API materials of the Builder's Goggles' view and the
 * Secret Goggles' overlay, found on the client thread once a renderer is up
 * and read on chunk-builder threads.
 */
@Environment(EnvType.CLIENT)
public final class FabricMaterials {

	private static volatile RenderMaterial fade;
	private static volatile RenderMaterial real;
	private static volatile RenderMaterial realFlat;
	private static volatile RenderMaterial outline;
	private static volatile RenderMaterial overlay;

	private FabricMaterials() {
	}

	/** The Builder's Goggles' materials; whether the view can be drawn (it cannot without a renderer). */
	public static boolean ensureBuilderView() {
		Renderer renderer;
		if (fade == null && (renderer = renderer()) != null) {
			real = renderer.materialFinder().find();
			realFlat = renderer.materialFinder().ambientOcclusion(TriState.FALSE).find();
			outline = renderer.materialFinder().blendMode(BlendMode.SOLID)
					.emissive(true).disableDiffuse(true).ambientOcclusion(TriState.FALSE).find();
			fade = renderer.materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
		}
		return fade != null;
	}

	/** The Secret Goggles' overlay material, tried each client tick until a renderer is up. */
	public static void ensureOverlay() {
		Renderer renderer;
		if (overlay == null && (renderer = renderer()) != null) {
			overlay = renderer.materialFinder()
					.blendMode(BlendMode.TRANSLUCENT)
					.emissive(true)
					.disableDiffuse(true)
					.ambientOcclusion(TriState.FALSE)
					.find();
		}
	}

	/** The active Renderer API implementation, or null before one is registered. */
	static Renderer renderer() {
		try {
			return Renderer.get();
		} catch (UnsupportedOperationException noneYet) {
			return null;
		}
	}

	/** Solid and full-bright (no shading or ambient occlusion), for the outline. */
	static RenderMaterial outline() {
		return outline;
	}

	/** Translucent (no emissive light), for the faded disguise. */
	static RenderMaterial fade() {
		return fade;
	}

	/**
	 * For the real block drawn inside: the renderer's standard material, or one
	 * without ambient occlusion for a model that has none, as vanilla draws it.
	 */
	static RenderMaterial real(boolean ambientOcclusion) {
		return ambientOcclusion ? real : realFlat;
	}

	/** Translucent, full-bright (emissive, no shading or ambient occlusion); null before a renderer is up. */
	static RenderMaterial overlay() {
		return overlay;
	}
}
