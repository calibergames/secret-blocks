package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class SecretBlockUnbakedModel implements UnbakedModel {

	private static final ResourceLocation DEFAULT_BLOCK_MODEL = ResourceLocation.parse("minecraft:block/block");

	@Override
	public void resolveDependencies(Resolver resolver) {
		resolver.resolve(DEFAULT_BLOCK_MODEL);
	}

	/**
	 * Held and shown as items like any full block: vanilla's block/block display transforms.
	 * The disguise brings its own models when drawn.
	 */
	@Override
	public BakedModel bake(TextureSlots textures, ModelBaker baker, ModelState rotationContainer, boolean ambientOcclusion,
			boolean lightLikeBlock, ItemTransforms transforms) {
		return new SecretBlockBakedModel(baker.bake(DEFAULT_BLOCK_MODEL, rotationContainer).getTransforms());
	}

}
