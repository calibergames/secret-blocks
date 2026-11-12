package net.calibermc.secretblocks.forge;

import net.calibermc.secretblocks.forge.client.ForgeClientRendering;
import net.calibermc.secretblocks.registry.ModEntities;
import net.calibermc.secretblocks.render.BuilderChestRenderer;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.screen.SecretChestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup, reached only on a client. */
final class SecretBlocksForgeClient {

	private SecretBlocksForgeClient() {
	}

	static void init(BusGroup modBus) {
		ModelEvent.ModifyBakingResult.BUS.addListener(ForgeClientRendering::modifyBakingResult);
		ModelEvent.BakingCompleted.BUS.addListener(ForgeClientRendering::modelsBaked);
		RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(BuilderView.TOGGLE_KEY));
		EntityRenderersEvent.RegisterRenderers.BUS.addListener(event ->
				event.registerBlockEntityRenderer(ModEntities.SECRET_BLOCK_ENTITY, BuilderChestRenderer::new));
		FMLClientSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(() -> {
			MenuScreens.register(ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER, SecretChestScreen::new);
			// Each quad's chunk layer comes from its texture, so the Torch Lever's copies of vanilla's
			// torch models need no layer of their own.
		}));
		TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
			Minecraft minecraft = Minecraft.getInstance();
			GogglesView.tick(minecraft);
			BuilderView.tick(minecraft);
		});
	}
}
