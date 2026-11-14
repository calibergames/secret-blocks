package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Supplies the secret blocks' shared model ({@code block/camo_block}), which draws each block's disguise. */
@Environment(EnvType.CLIENT)
public final class SecretBlockResourceProvider implements ModelLoadingPlugin {

	public static final SecretBlockUnbakedModel CAMO_MODEL = new SecretBlockUnbakedModel();
	public static final ResourceLocation CAMO_BLOCK = SecretBlocks.id("block/camo_block");

	/** After the phase in which other mods wrap the baked models. */
	private static final ResourceLocation OWN_MODEL_PHASE = SecretBlocks.id("own_model");

	@Override
	public void initialize(Context context) {
		// A secret block keeps its own model, without the wrappers other mods put
		// around block models (what those do to the block as a whole is dropped).
		// A connected-textures wrapper (Continuity) treats the whole block as one
		// look and then stands aside for the models drawn inside it, so a face
		// whose look differs from the bottom face would never connect. Each face
		// is drawn by its look's own model, which carries those wrappers itself.
		context.modifyBlockModelAfterBake().addPhaseOrdering(ModelModifier.WRAP_LAST_PHASE, OWN_MODEL_PHASE);
		context.modifyBlockModelAfterBake().register(OWN_MODEL_PHASE,
				(model, bake) -> BuiltInRegistries.BLOCK.getValue(bake.id().id()) instanceof SecretBlock
						? new SecretBlockBakedModel(model.getTransforms()) : model);
		// No JSON file backs this id: the model is supplied when it is requested.
		context.modifyModelOnLoad().register((model, load) -> load.id().equals(CAMO_BLOCK) ? CAMO_MODEL : model);
	}
}
