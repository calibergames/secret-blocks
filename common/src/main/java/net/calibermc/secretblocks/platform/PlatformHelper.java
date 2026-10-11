package net.calibermc.secretblocks.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/** What the mod needs from the loader it runs on. */
public interface PlatformHelper {

	/**
	 * Registers {@code value} under {@code id} and returns it. A loader that
	 * registers in its own registration phase may do it later, but before the
	 * game uses the registry.
	 */
	<V, T extends V> T register(Registry<V> registry, ResourceLocation id, T value);

	/** A creative tab builder placed by the loader after vanilla's tabs. */
	CreativeModeTab.Builder creativeTab();
}
