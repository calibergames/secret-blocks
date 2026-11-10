package net.calibermc.secretblocks.forge.client;

import net.calibermc.secretblocks.blocks.SecretBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

/** Puts the disguise model on every secret block and tells it of every model bake (the model types stay in this package). */
public final class ForgeClientRendering {

	private ForgeClientRendering() {
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
					event.getResults().blockStateModels().put(state, ForgeDisguiseModel.INSTANCE);
				}
			}
		}
	}

	/** The secret blocks' disguise quads carry their colours in their tint indices ({@link QuadTints}). */
	public static void registerColors(RegisterColorHandlersEvent.Block event) {
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block instanceof SecretBlock) {
				event.register((state, level, pos, tintIndex) -> QuadTints.color(tintIndex), block);
			}
		}
	}

	public static void modelsBaked(ModelEvent.BakingCompleted event) {
		ForgeDisguiseModel.modelsBaked();
	}
}
