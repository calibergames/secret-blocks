package net.calibermc.secretblocks.neoforge;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.screen.SecretChestScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup, reached only on a client. */
final class SecretBlocksNeoForgeClient {

	private SecretBlocksNeoForgeClient() {
	}

	static void init(IEventBus modBus) {
		modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
			MenuScreens.register(ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER, SecretChestScreen::new);
			// The Torch Lever draws vanilla's torch models, which need the cutout layer as vanilla's torch does.
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.TORCH_LEVER, RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(ModBlocks.WALL_TORCH_LEVER, RenderType.cutout());
		}));
	}
}
