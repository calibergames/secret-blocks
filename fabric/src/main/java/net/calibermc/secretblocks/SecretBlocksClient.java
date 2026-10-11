package net.calibermc.secretblocks;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModEntities;
import net.calibermc.secretblocks.render.BuilderChestRenderer;
import net.calibermc.secretblocks.render.BuilderView;
import net.calibermc.secretblocks.render.DisguiseMaterials;
import net.calibermc.secretblocks.render.GogglesView;
import net.calibermc.secretblocks.render.SecretBlockResourceProvider;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.screen.SecretChestScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;



@Environment(EnvType.CLIENT)
public class SecretBlocksClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {


		MenuScreens.register(ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER, SecretChestScreen::new);  //Screen handler
		ModelLoadingRegistry.INSTANCE.registerResourceProvider(rm -> new SecretBlockResourceProvider());
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.secretBlocksList);
		// The Torch Lever draws vanilla's torch models (tilted copies of them when on), which need the cutout layer as vanilla's torch does.
		BlockRenderLayerMap.INSTANCE.putBlocks(RenderType.cutout(), ModBlocks.TORCH_LEVER, ModBlocks.WALL_TORCH_LEVER);
		GogglesView.register();
		BuilderView.register();
		DisguiseMaterials.register();
		BlockEntityRendererRegistry.register(ModEntities.SECRET_BLOCK_ENTITY, BuilderChestRenderer::new);
//		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), SecretBlocks.glassBlocksList);
	}
}
