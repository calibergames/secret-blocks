package net.calibermc.secretblocks.gametest.driver.mixin;

import net.calibermc.secretblocks.gametest.driver.Recorder;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records each sound the client asks to play (development driver only). */
@Mixin(SoundManager.class)
public abstract class SoundRecorderMixin {

	@Inject(method = "play", at = @At("HEAD"))
	private void secretblocksTest$record(SoundInstance sound, CallbackInfo ci) {
		if (Recorder.isOn()) {
			// Only what is known before the manager resolves the sound (volume and pitch are not).
			Recorder.sound(String.format("%s at %.1f %.1f %.1f", sound.getLocation(), sound.getX(), sound.getY(), sound.getZ()));
		}
	}
}
