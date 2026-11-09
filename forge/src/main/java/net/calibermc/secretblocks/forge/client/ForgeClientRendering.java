package net.calibermc.secretblocks.forge.client;

import net.minecraftforge.client.event.ModelEvent;

/** Registers the disguise model's loader and tells it of every model bake (the model types stay in this package). */
public final class ForgeClientRendering {

	private ForgeClientRendering() {
	}

	public static void registerLoader(ModelEvent.RegisterGeometryLoaders event) {
		event.register("camo", CamoGeometry.LOADER);
	}

	public static void modelsBaked(ModelEvent.BakingCompleted event) {
		ForgeDisguiseModel.modelsBaked();
	}
}
