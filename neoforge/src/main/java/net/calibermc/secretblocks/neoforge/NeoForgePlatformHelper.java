package net.calibermc.secretblocks.neoforge;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.platform.PlatformHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link PlatformHelper} on NeoForge and Forge 1.20.1. Entries are held until
 * their registry's {@link RegisterEvent}; the common start runs at the first
 * registry event, so every entry is waiting before its registry's turn.
 */
public class NeoForgePlatformHelper implements PlatformHelper {

	private record Entry(ResourceLocation id, Object value) {
	}

	private static final Map<ResourceKey<?>, List<Entry>> PENDING = new HashMap<>();
	private static final Set<ResourceKey<?>> DONE = new HashSet<>();
	private static boolean started;

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public <V, T extends V> T register(Registry<V> registry, ResourceLocation id, T value) {
		if (DONE.contains(registry.key())) {
			throw new IllegalStateException("Too late to register " + id + ": " + registry.key().location() + " has already taken its entries");
		}
		PENDING.computeIfAbsent(registry.key(), key -> new ArrayList<>()).add(new Entry(id, value));
		return value;
	}

	@Override
	public CreativeModeTab.Builder creativeTab() {
		return CreativeModeTab.builder();
	}

	/** After loading: an entry still held went to a registry that never took entries. */
	static void checkNothingLeft() {
		if (!PENDING.isEmpty()) {
			throw new IllegalStateException("Never registered, their registries took no entries: " + PENDING);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static void onRegister(RegisterEvent event) {
		if (!started) {
			started = true;
			SecretBlocks.init();
		}
		ResourceKey key = event.getRegistryKey();
		DONE.add(key);
		List<Entry> entries = PENDING.remove(key);
		if (entries != null) {
			event.register(key, helper -> entries.forEach(entry -> ((RegisterEvent.RegisterHelper) helper).register(entry.id(), entry.value())));
		}
	}
}
