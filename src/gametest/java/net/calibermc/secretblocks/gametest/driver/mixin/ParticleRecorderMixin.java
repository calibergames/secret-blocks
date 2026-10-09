package net.calibermc.secretblocks.gametest.driver.mixin;

import net.calibermc.secretblocks.gametest.driver.Recorder;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the sprite of each block particle the client adds (development driver only). */
@Mixin(ParticleEngine.class)
public abstract class ParticleRecorderMixin {

	@Inject(method = "add", at = @At("HEAD"))
	private void secretblocksTest$record(Particle particle, CallbackInfo ci) {
		if (Recorder.isOn() && particle instanceof TerrainParticle terrain) {
			Recorder.particle(String.valueOf(((TextureSheetParticleAccessor) terrain).secretblocksTest$getSprite().getName()));
		}
	}
}
