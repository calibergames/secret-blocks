package net.calibermc.secretblocks;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
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
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;



@Environment(EnvType.CLIENT)
public class SecretBlocksClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {


		MenuScreens.register(ModScreenHandlers.SECRET_CHEST_SCREEN_HANDLER, SecretChestScreen::new);  //Screen handler
		ModelLoadingPlugin.register(new SecretBlockResourceProvider());
		// Each quad's chunk layer comes from its texture (from 26.1), so the disguises and the
		// Torch Lever's copies of vanilla's torch models need no layer of their own.
		KeyMappingHelper.registerKeyMapping(BuilderView.TOGGLE_KEY);
		BuilderView.setRendererCheck(FabricMaterials::ensureBuilderView);
		ClientTickEvents.END_CLIENT_TICK.register(GogglesView::tick);
		ClientTickEvents.END_CLIENT_TICK.register(BuilderView::tick);
		BlockEntityRendererRegistry.register(ModEntities.SECRET_BLOCK_ENTITY, BuilderChestRenderer::new);
//		BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), SecretBlocks.glassBlocksList);
	}
}
