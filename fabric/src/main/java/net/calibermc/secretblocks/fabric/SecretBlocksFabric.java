package net.calibermc.secretblocks.fabric;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.Opacity;
import net.calibermc.secretblocks.items.CamouflageWand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/** The Fabric entry point: the common start, then the Fabric events it needs. */
public class SecretBlocksFabric implements ModInitializer {

	@Override
	public void onInitialize() {
		SecretBlocks.init();
		// The wand acts before the clicked block's own use.
		UseBlockCallback.EVENT.register(CamouflageWand::onUseBlock);
		// Vanilla's End City treasure only, not a data pack's replacement.
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (source.isBuiltin() && SecretBlocks.getsGogglesLoot(key.identifier())) {
				tableBuilder.withPool(SecretBlocks.gogglesLootPool());
			}
		});
		// Each secret block in a chunk is checked as it loads (reading the chunk
		// itself: asking the level for a chunk while it loads would wait on it
		// forever) and put right after the tick, when the chunk may change.
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> Opacity.checkChunk(level, chunk));
		ServerTickEvents.END_LEVEL_TICK.register(Opacity::putRight);
		ServerLevelEvents.UNLOAD.register((server, level) -> Opacity.forget(level));
	}
}
