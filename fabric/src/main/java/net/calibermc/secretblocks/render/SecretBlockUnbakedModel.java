package net.calibermc.secretblocks.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;

/** The secret blocks' model before baking: it needs no other model or texture. */
@Environment(EnvType.CLIENT)
public class SecretBlockUnbakedModel implements BlockStateModel.Unbaked {

	@Override
	public void resolveDependencies(Resolver resolver) {
	}

	@Override
	public BlockStateModel bake(ModelBaker baker) {
		return new SecretBlockModel();
	}
}
