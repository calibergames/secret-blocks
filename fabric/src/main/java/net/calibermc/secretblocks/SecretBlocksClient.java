package net.calibermc.secretblocks;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModEntities;
import net.calibermc.secretblocks.render.BuilderChestRenderer;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.FabricMaterials;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.render.SecretBlockResourceProvider;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.screen.SecretChestScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;



@Environment(EnvType.CLIENT)
public class SecretBlocksClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {


		MenuScreens.register(ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER, SecretChestScreen::new);  //Screen handler
		ModelLoadingPlugin.register(new SecretBlockResourceProvider());
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, ModBlocks.secretBlocksList);
		// The Torch Lever draws vanilla's torch models (tilted copies of them when on), which need the cutout layer as vanilla's torch does.
		BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, ModBlocks.TORCH_LEVER, ModBlocks.WALL_TORCH_LEVER);
		KeyBindingHelper.registerKeyBinding(BuilderView.TOGGLE_KEY);
		BuilderView.setRendererCheck(FabricMaterials::ensureBuilderView);
		ClientTickEvents.END_CLIENT_TICK.register(GogglesView::tick);
		ClientTickEvents.END_CLIENT_TICK.register(BuilderView::tick);
		BlockEntityRendererRegistry.register(ModEntities.SECRET_BLOCK_ENTITY, BuilderChestRenderer::new);
//		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), SecretBlocks.glassBlocksList);
	}
}
