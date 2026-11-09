package net.calibermc.secretblocks.neoforge;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.Opacity;
import net.calibermc.secretblocks.items.CamouflageWand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The NeoForge entry point: the common start, run from the first
 * registry event (objects may only be made while the registries take them),
 * then the game events the mod needs.
 */
@Mod(SecretBlocks.MOD_ID)
public class SecretBlocksNeoForge {

	public SecretBlocksNeoForge(IEventBus modBus) {
		modBus.addListener(SecretBlocksNeoForge::onRegister);
		modBus.addListener((FMLLoadCompleteEvent event) -> NeoForgePlatformHelper.checkNothingLeft());
		if (FMLEnvironment.dist.isClient()) {
			SecretBlocksNeoForgeClient.init(modBus);
		}

		IEventBus bus = NeoForge.EVENT_BUS;
		// The wand acts before the clicked block's own use.
		bus.addListener(SecretBlocksNeoForge::onRightClickBlock);
		// Fired for the game's own tables only, not a data pack's replacement.
		bus.addListener((LootTableLoadEvent event) -> {
			if (SecretBlocks.getsGogglesLoot(event.getName())) {
				event.getTable().addPool(SecretBlocks.gogglesLootPool().name(SecretBlocks.MOD_ID + ":goggles").build());
			}
		});
		// Each secret block in a chunk is checked as it loads (reading the chunk
		// itself: asking the level for a chunk while it loads would wait on it
		// forever) and put right after the tick, when the chunk may change.
		bus.addListener((ChunkEvent.Load event) -> {
			if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
				Opacity.checkChunk(level, chunk);
			}
		});
		bus.addListener((LevelTickEvent.Post event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				Opacity.putRight(level);
			}
		});
		bus.addListener((LevelEvent.Unload event) -> {
			if (event.getLevel() instanceof ServerLevel level) {
				Opacity.forget(level);
			}
		});
	}

	private static void onRegister(RegisterEvent event) {
		NeoForgePlatformHelper.onRegister(event);
	}

	private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		InteractionResult result = CamouflageWand.onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
		if (result != InteractionResult.PASS) {
			event.setCancellationResult(result);
			event.setCanceled(true);
		}
	}
}
