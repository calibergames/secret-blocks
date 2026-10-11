package net.calibermc.secretblocks.neoforge.client;

import net.minecraftforge.client.event.ModelEvent;

/** Registers the disguise model's loader and tells it of every model bake (the model types stay in this package). */
public final class NeoForgeClientRendering {

	private NeoForgeClientRendering() {
	}

	public static void registerLoader(ModelEvent.RegisterGeometryLoaders event) {
		event.register("camo", CamoGeometry.LOADER);
	}

	public static void modelsBaked(ModelEvent.BakingCompleted event) {
		NeoForgeDisguiseModel.modelsBaked();
	}
}
