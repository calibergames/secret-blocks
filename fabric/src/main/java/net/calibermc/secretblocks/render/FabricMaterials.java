package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

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
		if (fade == null && RendererAccess.INSTANCE.hasRenderer()) {
			real = RendererAccess.INSTANCE.getRenderer().materialFinder().find();
			realFlat = RendererAccess.INSTANCE.getRenderer().materialFinder().disableAo(0, true).find();
			outline = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(0, BlendMode.SOLID)
					.emissive(0, true).disableDiffuse(0, true).disableAo(0, true).find();
			fade = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(0, BlendMode.TRANSLUCENT).find();
		}
		return fade != null;
	}

	/** The Secret Goggles' overlay material, tried each client tick until a renderer is up. */
	public static void ensureOverlay() {
		if (overlay == null && RendererAccess.INSTANCE.hasRenderer()) {
			overlay = RendererAccess.INSTANCE.getRenderer().materialFinder()
					.blendMode(0, BlendMode.TRANSLUCENT)
					.emissive(0, true)
					.disableDiffuse(0, true)
					.disableAo(0, true)
					.find();
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
