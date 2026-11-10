package net.calibermc.secretblocks.neoforge.client;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Puts the disguise model on every secret block and tells it of every model bake (the model types stay in this package). */
public final class NeoForgeClientRendering {

	private NeoForgeClientRendering() {
	}

	/**
	 * Every state of every secret block draws its disguise. Their block state
	 * files name {@code block/camo_block}, a plain model that only stands in
	 * until here.
	 */
	public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block instanceof SecretBlock) {
				for (BlockState state : block.getStateDefinition().getPossibleStates()) {
					event.getBakingResult().blockStateModels().put(state, NeoForgeDisguiseModel.INSTANCE);
				}
			}
		}
	}

	public static void modelsBaked(ModelEvent.BakingCompleted event) {
		NeoForgeDisguiseModel.modelsBaked();
	}
}
