package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.function.BooleanSupplier;

/**
 * Client side of the Builder's Goggles' view: while active, each secret
 * block's disguise is drawn faded (about 40%) with a thin, bright outline,
 * and its real vanilla block is drawn inside it, so a builder sees the wire,
 * the repeater's arrow, the chest, and where every secret block is.
 *
 * On while the local player wears the Builder's Goggles, unless switched
 * off with the toggle key, or while the development driver forces it
 * ({@link #setForced}). Putting
 * them on or taking them off, or switching the view, redraws every secret
 * block in range. Client
 * side only, for the wearer only: it shows what the client already knows.
 */
public final class BuilderView {

	/** How opaque the disguise is while the view is active (about 40%). */
	public static final int DISGUISE_ALPHA = 0x66;

	// Written on the client thread; read on chunk-builder threads.
	private static volatile boolean forced;
	private static volatile boolean showing;
	private static final BuilderViewToggle TOGGLE = new BuilderViewToggle();
	/**
	 * Switches the view off and on while the goggles are worn (Controls,
	 * "Secret Blocks"; B by default). Each loader registers it.
	 */
	public static final KeyMapping TOGGLE_KEY = new KeyMapping("key.secretblocks.builder_view",
			InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.secretblocks");
	/** Whether the loader's renderer can draw the view (Fabric needs a Fabric Renderer API implementation). */
	private static volatile BooleanSupplier rendererCheck = () -> true;
	private static volatile boolean rendererReady;
	private static volatile TextureAtlasSprite outlineSprite;
	private static volatile TextureAtlasSprite ghostSprite;
	private static volatile boolean shaderPack;

	/** Plain white, tinted for the outline drawn around every secret block. */
	public static final ResourceLocation OUTLINE_SPRITE = new ResourceLocation(SecretBlocks.MOD_ID, "block/builder_outline");
	/** Translucent in the texture itself: the faded block under a shader pack. */
	public static final ResourceLocation GHOST_SPRITE = new ResourceLocation(SecretBlocks.MOD_ID, "block/builder_ghost");

	private BuilderView() {
	}

	/**
	 * The loader's check that its renderer can draw the view, run on the
	 * client thread when the view may switch on.
	 */
	public static void setRendererCheck(BooleanSupplier check) {
		rendererCheck = check;
	}

	public static boolean isActive() {
		return (forced || showing) && rendererReady;
	}

	/**
	 * Each client tick, from the loader's end-of-tick event. OUTLINE_SPRITE and
	 * GHOST_SPRITE are in textures/block/, which the block atlas takes in whole.
	 */
	public static void tick(Minecraft mc) {
		// Only in a world (the client also ticks during the first resource load).
		if (mc.level == null) {
			// Out of a world the goggles are not worn: the next world starts with the view on.
			TOGGLE.setWearing(false);
			showing = false;
			return;
		}
		// Refreshed every tick so a resource reload (new atlas) is picked up.
		outlineSprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(OUTLINE_SPRITE);
		ghostSprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(GHOST_SPRITE);
		boolean packNow = ShaderPacks.inUse();
		if (packNow != shaderPack) {
			shaderPack = packNow;
			if (isActive()) {
				GogglesView.redrawSecretBlocks(mc);
			}
		}
		boolean worn = mc.player != null && mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BUILDERS_GOGGLES);
		boolean changed = TOGGLE.setWearing(worn);
		while (TOGGLE_KEY.consumeClick()) {
			if (TOGGLE.toggle()) {
				changed = true;
				mc.player.displayClientMessage(Component.translatable(TOGGLE.showing()
						? "message.secretblocks.builder_view.on" : "message.secretblocks.builder_view.off"), true);
			}
		}
		if (changed) {
			rendererReady = rendererCheck.getAsBoolean();
			showing = TOGGLE.showing();
			GogglesView.redrawSecretBlocks(mc);
		}
	}

	/**
	 * Whether a shader pack is in use. Shader packs draw translucent terrain
	 * with the texture's own alpha and ignore the vertex colour's, so the
	 * faded disguise would come out solid and hide the real block: with a
	 * pack the faded block is drawn with {@link #ghostSprite} instead.
	 */
	public static boolean shaderPack() {
		return shaderPack;
	}

	/** The faded block's sprite under a shader pack, or null before the first client tick in a world. */
	public static TextureAtlasSprite ghostSprite() {
		return ghostSprite;
	}

	/** The outline's sprite, or null before the first client tick in a world. */
	public static TextureAtlasSprite outlineSprite() {
		return outlineSprite;
	}

	/**
	 * Switch the view on or off (client thread) and redraw every secret block
	 * in range. Returns whether the view is now active: it cannot be where the
	 * loader's renderer cannot draw it (on Fabric, without a Fabric Renderer
	 * API implementation, e.g. Sodium without Indium).
	 */
	public static boolean setForced(boolean on) {
		rendererReady = rendererCheck.getAsBoolean();
		forced = on;
		GogglesView.redrawSecretBlocks(Minecraft.getInstance());
		return isActive();
	}
}
