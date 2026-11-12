package net.calibermc.secretblocks.neoforge;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.calibermc.secretblocks.neoforge.client.NeoForgeClientRendering;
import net.calibermc.secretblocks.registry.ModEntities;
import net.calibermc.secretblocks.render.BuilderChestRenderer;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.screen.SecretChestScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;

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
		// Each quad's chunk layer comes from its texture, so the Torch Lever's copies of vanilla's torch models need no layer of their own.
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
			Minecraft minecraft = Minecraft.getInstance();
			GogglesView.tick(minecraft);
			BuilderView.tick(minecraft);
		});
	}
}
