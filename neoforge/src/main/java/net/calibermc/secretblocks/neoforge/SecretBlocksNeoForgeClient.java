package net.calibermc.secretblocks.neoforge;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.calibermc.secretblocks.neoforge.client.NeoForgeClientRendering;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModEntities;
import net.calibermc.secretblocks.render.BuilderChestRenderer;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.screen.SecretChestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup, reached only on a client. */
final class SecretBlocksNeoForgeClient {

	private SecretBlocksNeoForgeClient() {
	}

	static void init(IEventBus modBus) {
		modBus.addListener((ModelEvent.ModifyBakingResult event) -> NeoForgeClientRendering.modifyBakingResult(event));
		modBus.addListener((ModelEvent.BakingCompleted event) -> NeoForgeClientRendering.modelsBaked(event));
		modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(BuilderView.TOGGLE_KEY));
		modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
				event.registerBlockEntityRenderer(ModEntities.SECRET_BLOCK_ENTITY, BuilderChestRenderer::new));
		modBus.addListener((RegisterMenuScreensEvent event) ->
				event.register(ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER, SecretChestScreen::new));
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			// The Torch Lever draws vanilla's torch models, which need the cutout layer as vanilla's torch does.
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.TORCH_LEVER, ChunkSectionLayer.CUTOUT);
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.WALL_TORCH_LEVER, ChunkSectionLayer.CUTOUT);
		}));
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
			Minecraft minecraft = Minecraft.getInstance();
			GogglesView.tick(minecraft);
			BuilderView.tick(minecraft);
		});
	}
}
