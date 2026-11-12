package net.calibermc.secretblocks.gametest.support.mixin;

import net.calibermc.secretblocks.gametest.support.RegistryLoadContext;
import net.calibermc.secretblocks.gametest.support.SharedGameTests;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the shared game tests to the data-pack registries as a server loads
 * them from its data packs, beside the ones read from data: the environments
 * to their registry, and the test instances to theirs, naming the
 * environments through the lookup the load shares (registries load side by
 * side, so an environment may be registered after the test that names it).
 * This is the same on every loader, in development and on production game
 * test servers.
 */
@Mixin(RegistryLoadTask.class)
public abstract class TestRegistriesMixin implements RegistryLoadContext {

	@Shadow
	@Final
	private Object registryWriteLock;

	@Shadow
	@Final
	private WritableRegistry<?> registry;

	/** The lookup a load from data packs shares ({@code ResourceManagerLoadMixin}); null for other loads. */
	@Unique
	@Nullable
	private RegistryOps.RegistryInfoLookup secretblocksTest$context;

	@Override
	public void secretblocksTest$setContext(RegistryOps.RegistryInfoLookup context) {
		secretblocksTest$context = context;
	}

	@SuppressWarnings("unchecked")
	@Inject(method = "registerElements", at = @At("TAIL"))
	private void secretblocksTest$addSharedTests(CallbackInfo ci) {
		RegistryOps.RegistryInfoLookup context = secretblocksTest$context;
		if (context == null) {
			return;
		}
		synchronized (registryWriteLock) {
			if (registry.key().equals(Registries.TEST_ENVIRONMENT)) {
				SharedGameTests.registerEnvironments((WritableRegistry<TestEnvironmentDefinition<?>>) registry);
			} else if (registry.key().equals(Registries.TEST_INSTANCE)) {
				SharedGameTests.registerInstances((WritableRegistry<GameTestInstance>) registry,
						context.<TestEnvironmentDefinition<?>>lookup(Registries.TEST_ENVIRONMENT).orElseThrow().getter());
			}
		}
	}
}
