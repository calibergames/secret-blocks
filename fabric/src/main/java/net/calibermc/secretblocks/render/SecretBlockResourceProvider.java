package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gives every state of every secret block the disguise model; their block
 * state files (which name {@code block/camo_block}) are not read.
 */
@Environment(EnvType.CLIENT)
public final class SecretBlockResourceProvider implements ModelLoadingPlugin {

	private static final BlockStateModel.UnbakedRoot CAMO_MODEL = new SecretBlockUnbakedModel().asRoot();

	/** After the phase in which other mods wrap the baked models. */
	private static final Identifier OWN_MODEL_PHASE = SecretBlocks.id("own_model");

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
				(model, bake) -> bake.state().getBlock() instanceof SecretBlock ? new SecretBlockModel() : model);
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block instanceof SecretBlock) {
				context.registerBlockStateResolver(block, states -> {
					for (BlockState state : block.getStateDefinition().getPossibleStates()) {
						states.setModel(state, CAMO_MODEL);
					}
				});
			}
		}
	}
}
