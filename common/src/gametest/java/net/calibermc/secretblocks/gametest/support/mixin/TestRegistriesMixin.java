package net.calibermc.secretblocks.gametest.support.mixin;

import java.util.Map;

import com.mojang.serialization.Decoder;
import net.calibermc.secretblocks.gametest.support.SharedGameTests;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the shared game tests to the data-pack registries as a server loads
 * them from its data packs, beside the ones read from data: the environments
 * first (that registry loads first), then the test instances. This is the same
 * on every loader, in development and on production game test servers.
 */
@Mixin(RegistryDataLoader.class)
public abstract class TestRegistriesMixin {

	@SuppressWarnings("unchecked")
	@Inject(method = "loadContentsFromManager", at = @At("HEAD"))
	private static <E> void secretblocks$addSharedTests(ResourceManager resources, RegistryOps.RegistryInfoLookup lookup,
			WritableRegistry<E> registry, Decoder<E> decoder, Map<ResourceKey<?>, Exception> errors, CallbackInfo ci) {
		if (registry.key().equals(Registries.TEST_ENVIRONMENT)) {
			SharedGameTests.registerEnvironments((WritableRegistry<TestEnvironmentDefinition>) registry);
		} else if (registry.key().equals(Registries.TEST_INSTANCE)) {
			SharedGameTests.registerInstances((WritableRegistry<GameTestInstance>) registry,
					lookup.<TestEnvironmentDefinition>lookup(Registries.TEST_ENVIRONMENT).orElseThrow().getter());
		}
	}
}
