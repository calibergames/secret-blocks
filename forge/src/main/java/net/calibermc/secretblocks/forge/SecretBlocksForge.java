package net.calibermc.secretblocks.forge;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.Opacity;
import net.calibermc.secretblocks.items.CamouflageWand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.RegisterEvent;

/**
 * The Forge entry point: the common start, run from the first
 * registry event (objects may only be made while the registries take them),
 * then the game events the mod needs.
 */
@Mod(SecretBlocks.MOD_ID)
public class SecretBlocksForge {

	public SecretBlocksForge(FMLJavaModLoadingContext context) {
		BusGroup modBus = context.getModBusGroup();
		RegisterEvent.getBus(modBus).addListener(SecretBlocksForge::onRegister);
		FMLLoadCompleteEvent.getBus(modBus).addListener(event -> ForgePlatformHelper.checkNothingLeft());
		if (FMLEnvironment.dist.isClient()) {
			SecretBlocksForgeClient.init(modBus);
		}

		// The wand acts before the clicked block's own use.
		PlayerInteractEvent.RightClickBlock.BUS.addListener(SecretBlocksForge::onRightClickBlock);
		// Fired for the game's own tables only, not a data pack's replacement.
		LootTableLoadEvent.BUS.addListener((LootTableLoadEvent event) -> {
			if (SecretBlocks.getsGogglesLoot(event.getName())) {
				event.getTable().addPool(SecretBlocks.gogglesLootPool().name(SecretBlocks.MOD_ID + ":goggles").build());
			}
		});
		// Each secret block in a chunk is checked as it loads (reading the chunk
		// itself: asking the level for a chunk while it loads would wait on it
		// forever) and put right after the tick, when the chunk may change.
		ChunkEvent.Load.BUS.addListener(event -> {
			if (event.getChunk() instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level) {
				Opacity.checkChunk(level, chunk);
			}
		});
		TickEvent.LevelTickEvent.Post.BUS.addListener(event -> {
			if (event.level() instanceof ServerLevel level) {
				Opacity.putRight(level);
			}
		});
		LevelEvent.Unload.BUS.addListener(event -> {
			if (event.getLevel() instanceof ServerLevel level) {
				Opacity.forget(level);
			}
		});
	}

	private static void onRegister(RegisterEvent event) {
		ForgePlatformHelper.onRegister(event);
	}

	/** Cancels the click (the block is not used) when the wand acted. */
	private static boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		InteractionResult result = CamouflageWand.onUseBlock(event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
		if (result != InteractionResult.PASS) {
			event.setCancellationResult(result);
			return true;
		}
		return false;
	}
}
