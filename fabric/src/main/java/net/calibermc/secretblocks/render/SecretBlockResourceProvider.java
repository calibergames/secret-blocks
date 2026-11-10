package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.SecretBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;

/** Supplies the secret blocks' shared model ({@code block/camo_block}), which draws each block's disguise. */
@Environment(EnvType.CLIENT)
public final class SecretBlockResourceProvider implements ModelLoadingPlugin {

	public static final SecretBlockUnbakedModel CAMO_MODEL = new SecretBlockUnbakedModel();
	public static final ResourceLocation CAMO_BLOCK = SecretBlocks.id("block/camo_block");

	@Override
	public void initialize(Context context) {
		// No JSON file backs this id: the model is supplied when it is requested.
		context.modifyModelOnLoad().register((model, load) -> load.id().equals(CAMO_BLOCK) ? CAMO_MODEL : model);
	}
}
