package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelProviderException;
import net.fabricmc.fabric.api.client.model.ModelResourceProvider;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.calibermc.secretblocks.SecretBlocks;

@Environment(EnvType.CLIENT)
public class SecretBlockResourceProvider implements ModelResourceProvider {

	public static final SecretBlockUnbakedModel CAMO_MODEL = new SecretBlockUnbakedModel();
	public static final ResourceLocation CAMO_BLOCK = SecretBlocks.id("block/camo_block");

	@Override
	public UnbakedModel loadModelResource(ResourceLocation identifier, ModelProviderContext modelProviderContext) throws ModelProviderException {
		if (identifier.equals(CAMO_BLOCK)) {
			return CAMO_MODEL;
		} else {
			return null;
		}
	}
}
