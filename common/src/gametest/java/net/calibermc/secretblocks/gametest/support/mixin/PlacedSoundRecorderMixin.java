package net.calibermc.secretblocks.gametest.support.mixin;

import net.calibermc.secretblocks.gametest.support.PlayedSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the block sounds the server plays at a position (a placed block's) while {@link PlayedSounds} is on. */
@Mixin(Level.class)
public abstract class PlacedSoundRecorderMixin {

	@Inject(method = "playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
			at = @At("HEAD"))
	private void secretblocksTest$record(Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch, CallbackInfo ci) {
		PlayedSounds.record(sound, volume, pitch);
	}
}
