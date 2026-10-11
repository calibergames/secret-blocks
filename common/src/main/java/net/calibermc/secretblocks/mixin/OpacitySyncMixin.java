package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.Opacity;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Whenever a secret block's block entity changes (a new disguise, a probe,
 * the wand, a data command), its {@link Opacity#OPAQUE} follows its looks.
 */
@Mixin(BlockEntity.class)
public abstract class OpacitySyncMixin {

	@Shadow
	@Nullable
	protected Level level;

	@Shadow
	@Final
	protected BlockPos worldPosition;

	@Inject(method = "setChanged()V", at = @At("TAIL"))
	private void secretblocks$syncOpacity(CallbackInfo ci) {
		// The mixin's "this" is the block entity itself; only secret ones, and only the one actually at its place.
		if ((Object) this instanceof DisguisedBlockEntity && level != null && level.getBlockEntity(worldPosition) == (Object) this) {
			Opacity.sync(level, worldPosition);
		}
	}
}
