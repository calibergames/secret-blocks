package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class SecretBlockUnbakedModel implements UnbakedModel {

	private static final ResourceLocation DEFAULT_BLOCK_MODEL = new ResourceLocation("minecraft:block/block");

	@Override
	public Collection<ResourceLocation> getDependencies() {
		return Arrays.asList(DEFAULT_BLOCK_MODEL);
	}

	/** No parents: the disguise brings its own models when drawn. */
	@Override
	public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
	}

	/** Held and shown as items like any full block: vanilla's block/block display transforms. */
	@Override
	public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> textureGetter, ModelState rotationContainer, ResourceLocation modelId) {
		UnbakedModel block = baker.getModel(DEFAULT_BLOCK_MODEL);
		return new SecretBlockBakedModel(block instanceof BlockModel model ? model.getTransforms() : ItemTransforms.NO_TRANSFORMS);
	}

}
