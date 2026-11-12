package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
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

	@Override
	public void initialize(Context context) {
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
