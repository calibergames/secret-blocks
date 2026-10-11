package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;

/**
 * Client side of the Secret Goggles: while the local player wears them,
 * the disguise renderer adds a translucent, full-bright, animated rainbow
 * overlay to every disguise face. Putting them on or taking them off redraws
 * every secret block within render distance.
 */
public final class GogglesView {

	public static final ResourceLocation OVERLAY_SPRITE = new ResourceLocation(SecretBlocks.MOD_ID, "block/goggles_overlay");

	// Written on the client thread each tick; read on chunk-builder threads,
	// which must not touch the texture manager or the renderer themselves.
	private static volatile boolean wearing;
	private static volatile TextureAtlasSprite overlaySprite;

	private GogglesView() {
	}

	public static boolean isWearing() {
		return wearing;
	}

	/** The overlay sprite, or null before the first client tick. */
	public static TextureAtlasSprite overlaySprite() {
		return overlaySprite;
	}

	/**
	 * Each client tick, from the loader's end-of-tick event. OVERLAY_SPRITE is
	 * in textures/block/, which the block atlas takes in whole.
	 */
	public static void tick(Minecraft mc) {
		// Only in a world: the client also ticks during the first resource
		// load, before any texture atlas exists (asking for one then crashes).
		if (mc.level == null) {
			return;
		}
		// Refreshed every tick so a resource reload (new atlas) is picked up.
		overlaySprite = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(OVERLAY_SPRITE);
		boolean now = mc.player != null && mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SECRET_GOGGLES);
		if (now != wearing) {
			wearing = now;
			redrawSecretBlocks(mc);
		}
	}

	/** Mark every render section holding a secret block within render distance for a rebuild (each once). */
	public static void redrawSecretBlocks(Minecraft mc) {
		if (mc.level == null || mc.player == null) {
			return;
		}
		Set<Long> sections = new HashSet<>();
		ChunkPos center = mc.player.chunkPosition();
		int distance = mc.options.getEffectiveRenderDistance();
		for (int x = center.x - distance; x <= center.x + distance; x++) {
			for (int z = center.z - distance; z <= center.z + distance; z++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof DisguisedBlockEntity) {
						sections.add(SectionPos.asLong(entity.getBlockPos()));
					}
				}
			}
		}
		for (long section : sections) {
			mc.levelRenderer.setSectionDirty(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
		}
	}
}
