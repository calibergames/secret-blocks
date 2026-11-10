package net.calibermc.secretblocks.neoforge.client;

import net.calibermc.secretblocks.SecretBlocks;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Registers the disguise model's loader and tells it of every model bake (the model types stay in this package). */
public final class NeoForgeClientRendering {

	private NeoForgeClientRendering() {
	}

	public static void registerLoader(ModelEvent.RegisterLoaders event) {
		// The camo_block model names it as secretblocks:camo.
		event.register(SecretBlocks.id("camo"), CamoGeometry.LOADER);
	}

	public static void modelsBaked(ModelEvent.BakingCompleted event) {
		NeoForgeDisguiseModel.modelsBaked();
	}
}
