package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.items.SecretBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The place sound's volume and pitch come from the look of the secret block
 * being placed, as its sound does ({@link SecretBlockItem#placingLook()}).
 */
@Mixin(BlockItem.class)
public abstract class PlaceSoundLookMixin {

	@ModifyVariable(method = "place", at = @At("STORE"))
	private SoundType secretblocks$placeLook(SoundType sound) {
		BlockState look = SecretBlockItem.placingLook();
		return look != null ? look.getSoundType() : sound;
	}
}
