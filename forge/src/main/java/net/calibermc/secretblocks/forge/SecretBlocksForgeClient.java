package net.calibermc.secretblocks.forge;

import net.calibermc.secretblocks.forge.client.ForgeClientRendering;
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
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup, reached only on a client. */
final class SecretBlocksForgeClient {

	private SecretBlocksForgeClient() {
	}

	static void init(BusGroup modBus) {
		ModelEvent.ModifyBakingResult.BUS.addListener(ForgeClientRendering::modifyBakingResult);
		ModelEvent.BakingCompleted.BUS.addListener(ForgeClientRendering::modelsBaked);
		RegisterColorHandlersEvent.Block.getBus(modBus).addListener(ForgeClientRendering::registerColors);
		RegisterKeyMappingsEvent.getBus(modBus).addListener(event -> event.register(BuilderView.TOGGLE_KEY));
		EntityRenderersEvent.RegisterRenderers.getBus(modBus).addListener(event ->
				event.registerBlockEntityRenderer(ModEntities.SECRET_BLOCK_ENTITY, BuilderChestRenderer::new));
		FMLClientSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(() -> {
			MenuScreens.register(ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER, SecretChestScreen::new);
			// The Torch Lever draws vanilla's torch models, which need the cutout layer as vanilla's torch does.
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.TORCH_LEVER, ChunkSectionLayer.CUTOUT);
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.WALL_TORCH_LEVER, ChunkSectionLayer.CUTOUT);
		}));
		TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
			Minecraft minecraft = Minecraft.getInstance();
			GogglesView.tick(minecraft);
			BuilderView.tick(minecraft);
		});
	}
}
