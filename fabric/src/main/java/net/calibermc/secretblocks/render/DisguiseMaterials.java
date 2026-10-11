package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Materials for drawing disguises, found once the renderer is up (the first
 * client tick) and read on chunk-builder threads. A disguise face is drawn
 * in the pass its block is drawn in: stained glass translucent, leaves with
 * holes on Fancy graphics and solid on Fast, as vanilla draws them.
 */
@Environment(EnvType.CLIENT)
public final class DisguiseMaterials {

	private static volatile RenderMaterial solid;
	private static volatile RenderMaterial cutout;
	private static volatile RenderMaterial cutoutMipped;
	private static volatile RenderMaterial translucent;

	private DisguiseMaterials() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (translucent == null && RendererAccess.INSTANCE.hasRenderer()) {
				solid = find(BlendMode.SOLID);
				cutout = find(BlendMode.CUTOUT);
				cutoutMipped = find(BlendMode.CUTOUT_MIPPED);
				translucent = find(BlendMode.TRANSLUCENT);
			}
		});
	}

	private static RenderMaterial find(BlendMode mode) {
		return RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(0, mode).find();
	}

	/** The material for a disguise face showing {@code disguise}, or null before the first client tick. */
	static RenderMaterial forDisguise(BlockState disguise) {
		RenderType layer = ItemBlockRenderTypes.getChunkRenderType(disguise);
		if (layer == RenderType.translucent()) {
			return translucent;
		}
		if (layer == RenderType.cutoutMipped()) {
			return cutoutMipped;
		}
		if (layer == RenderType.cutout()) {
			return cutout;
		}
		return solid;
	}
}
