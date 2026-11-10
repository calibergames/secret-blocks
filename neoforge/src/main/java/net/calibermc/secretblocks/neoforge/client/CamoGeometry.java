package net.calibermc.secretblocks.neoforge.client;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/**
 * The {@code secretblocks:camo} model loader that {@code block/camo_block}
 * names: the disguise model, with the display transforms of its parent
 * ({@code block/block}).
 */
final class CamoGeometry extends AbstractUnbakedModel {

	static final UnbakedModelLoader<CamoGeometry> LOADER = (json, context) ->
			new CamoGeometry(StandardModelParameters.parse(json, context));

	private CamoGeometry(StandardModelParameters parameters) {
		super(parameters);
	}

	@Override
	public BakedModel bake(TextureSlots textures, ModelBaker baker, ModelState modelState, boolean useAmbientOcclusion,
			boolean usesBlockLight, ItemTransforms itemTransforms, ContextMap additionalProperties) {
		return new NeoForgeDisguiseModel(itemTransforms);
	}
}
