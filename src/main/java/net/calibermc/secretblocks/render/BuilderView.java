package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.registry.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Client side of the Builder's Goggles' view: while active, each secret
 * block's disguise is drawn faded (about 40%) and its real vanilla block is
 * drawn inside it, so a builder sees the wire, the repeater's arrow, the
 * chest.
 *
 * On while the local player wears the Builder's Goggles,
 * or while the development driver forces it ({@link #setForced}). Putting
 * them on or taking them off redraws every secret block in range. Client
 * side only, for the wearer only: it shows what the client already knows.
 */
@Environment(EnvType.CLIENT)
public final class BuilderView {

	/** How opaque the disguise is while the view is active (about 40%). */
	static final int DISGUISE_ALPHA = 0x66;

	// Written on the client thread; read on chunk-builder threads.
	private static volatile boolean forced;
	private static volatile boolean wearing;
	private static volatile RenderMaterial fadeMaterial;
	private static volatile RenderMaterial realMaterial;
	private static volatile RenderMaterial realFlatMaterial;

	private BuilderView() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(BuilderView::tick);
	}

	public static boolean isActive() {
		return (forced || wearing) && fadeMaterial != null;
	}

	private static void tick(Minecraft mc) {
		// Only in a world (the client also ticks during the first resource load).
		if (mc.level == null) {
			return;
		}
		boolean now = mc.player != null && mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BUILDERS_GOGGLES);
		if (now != wearing) {
			ensureMaterial();
			wearing = now;
			GogglesView.redrawSecretBlocks(mc);
		}
	}

	private static void ensureMaterial() {
		if (fadeMaterial == null && RendererAccess.INSTANCE.hasRenderer()) {
			realMaterial = RendererAccess.INSTANCE.getRenderer().materialFinder().find();
			realFlatMaterial = RendererAccess.INSTANCE.getRenderer().materialFinder().disableAo(0, true).find();
			fadeMaterial = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(0, BlendMode.TRANSLUCENT).find();
		}
	}

	/** Translucent (no emissive light), for the faded disguise; null until the view is first switched on. */
	static RenderMaterial fadeMaterial() {
		return fadeMaterial;
	}

	/**
	 * For the real block drawn inside (set with {@link #fadeMaterial}): the
	 * renderer's standard material, or one without ambient occlusion for a
	 * model that has none, as vanilla draws it.
	 */
	static RenderMaterial realMaterial(boolean ambientOcclusion) {
		return ambientOcclusion ? realMaterial : realFlatMaterial;
	}

	/**
	 * Switch the view on or off (client thread) and redraw every secret block
	 * in range. Returns whether the view is now active: it cannot be without a
	 * Fabric Renderer API implementation (e.g. Sodium without Indium).
	 */
	public static boolean setForced(boolean on) {
		ensureMaterial();
		forced = on;
		GogglesView.redrawSecretBlocks(Minecraft.getInstance());
		return isActive();
	}
}
