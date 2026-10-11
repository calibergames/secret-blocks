package net.calibermc.secretblocks.neoforge;

import net.calibermc.secretblocks.neoforge.client.NeoForgeClientRendering;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModEntities;
import net.calibermc.secretblocks.render.BuilderChestRenderer;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.screen.SecretChestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup, reached only on a client. */
final class SecretBlocksNeoForgeClient {

	private SecretBlocksNeoForgeClient() {
	}

	static void init(IEventBus modBus) {
		modBus.addListener((ModelEvent.RegisterGeometryLoaders event) -> NeoForgeClientRendering.registerLoader(event));
		modBus.addListener((ModelEvent.BakingCompleted event) -> NeoForgeClientRendering.modelsBaked(event));
		modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(BuilderView.TOGGLE_KEY));
		modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
				event.registerBlockEntityRenderer(ModEntities.SECRET_BLOCK_ENTITY, BuilderChestRenderer::new));
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			MenuScreens.register(ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER, SecretChestScreen::new);
			// The Torch Lever draws vanilla's torch models, which need the cutout layer as vanilla's torch does.
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.TORCH_LEVER, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.WALL_TORCH_LEVER, RenderType.cutout());
		}));
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				Minecraft minecraft = Minecraft.getInstance();
				GogglesView.tick(minecraft);
				BuilderView.tick(minecraft);
			}
		});
	}
}
