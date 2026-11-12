package net.calibermc.secretblocks.gametest.support;

import net.minecraft.resources.RegistryOps;

/** A registry load task that notes the lookup a load from data packs shares (see the test registry mixins). */
public interface RegistryLoadContext {

	void secretblocksTest$setContext(RegistryOps.RegistryInfoLookup context);
}
