package net.calibermc.secretblocks.gametest.support.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.calibermc.secretblocks.gametest.support.RegistryLoadContext;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceManagerRegistryLoadTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Notes the lookup a registry load from data packs shares, for {@code TestRegistriesMixin}. */
@Mixin(ResourceManagerRegistryLoadTask.class)
public abstract class ResourceManagerLoadMixin {

	@Inject(method = "load", at = @At("HEAD"))
	private void secretblocksTest$noteContext(RegistryOps.RegistryInfoLookup context, Executor executor,
			CallbackInfoReturnable<CompletableFuture<?>> cir) {
		((RegistryLoadContext) this).secretblocksTest$setContext(context);
	}
}
