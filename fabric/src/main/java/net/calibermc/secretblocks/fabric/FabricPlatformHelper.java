package net.calibermc.secretblocks.fabric;

import net.calibermc.secretblocks.platform.PlatformHelper;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/** {@link PlatformHelper} on Fabric: registries take entries at once; Fabric API places the creative tab. */
public class FabricPlatformHelper implements PlatformHelper {

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public <V, T extends V> T register(Registry<V> registry, ResourceLocation id, T value) {
		return Registry.register(registry, id, value);
	}

	@Override
	public CreativeModeTab.Builder creativeTab() {
		return FabricItemGroup.builder();
	}
}
