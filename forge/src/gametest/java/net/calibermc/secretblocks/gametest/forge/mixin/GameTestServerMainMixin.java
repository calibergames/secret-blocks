package net.calibermc.secretblocks.gametest.forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts a production Forge server as a game test server when
 * {@value #PROPERTY} is {@code true}. Forge's production server only starts
 * the dedicated server (its game test server is a development launch), so the
 * server's entry point hands over to vanilla's game test entry point, which
 * loads the mods, runs every test in a fresh flat world, writes the report
 * and exits with the number of failed required tests.
 */
@Mixin(net.minecraft.server.Main.class)
abstract class GameTestServerMainMixin {

	private static final String PROPERTY = "secretblocks.gametest.server";

	@Inject(method = "main", at = @At("HEAD"), cancellable = true)
	private static void secretblocks$runGameTests(String[] args, CallbackInfo ci) {
		if (!Boolean.getBoolean(PROPERTY)) {
			return;
		}
		ci.cancel();
		try {
			net.minecraft.gametest.Main.main(args);
		} catch (Exception e) {
			throw new IllegalStateException("The game test server failed to start", e);
		}
	}
}
