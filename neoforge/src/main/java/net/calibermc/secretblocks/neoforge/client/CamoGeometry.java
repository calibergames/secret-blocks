package net.calibermc.secretblocks.neoforge.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.List;
import java.util.function.Function;

/**
 * The {@code secretblocks:camo} model loader that {@code block/camo_block}
 * names: the disguise model, with the display transforms of its parent
 * ({@code block/block}).
 */
final class CamoGeometry implements IUnbakedGeometry<CamoGeometry> {

	static final IGeometryLoader<CamoGeometry> LOADER = new IGeometryLoader<>() {
		@Override
		public CamoGeometry read(JsonObject json, JsonDeserializationContext context) {
			return new CamoGeometry();
		}
	};

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
			ModelState modelState, List<ItemOverride> overrides) {
		return new NeoForgeDisguiseModel(context.getTransforms());
	}
}
