package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Continuity (connected textures, 2.x on 1.19.2) decides whether a block's
 * texture joins its neighbour by reading the neighbour's block state. Here a
 * secret neighbour reads as the look of its face on the same side, so real
 * glass joins secret glass beside it. A pseudo mixin, applied only with
 * Continuity 2.0.x installed (the version it was written against: another
 * may name its method differently); no build dependency on it.
 */
@Environment(EnvType.CLIENT)
@Pseudo
@Mixin(targets = "me.pepperbell.continuity.client.processor.ConnectionPredicate")
public interface ContinuityConnectionMixin {

	/** The call as the development environment names it (Mojang names). */
	@Redirect(method = "shouldConnect*", remap = false, require = 0, at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/world/level/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState secretblocks$neighbourLook(BlockAndTintGetter view, BlockPos toPos, BlockAndTintGetter blockView, BlockState state,
			BlockPos pos, BlockPos target, Direction face) {
		return DisguiseLook.looks(view, toPos, view.getBlockState(toPos), face);
	}

	/**
	 * The same call as a player's game names it: Continuity ships in
	 * intermediary names, and its class is not ours to remap.
	 */
	@Redirect(method = "shouldConnect*", remap = false, require = 0, at = @At(value = "INVOKE", remap = false,
			target = "Lnet/minecraft/class_1920;method_8320(Lnet/minecraft/class_2338;)Lnet/minecraft/class_2680;"))
	private BlockState secretblocks$neighbourLookInGame(BlockAndTintGetter view, BlockPos toPos, BlockAndTintGetter blockView, BlockState state,
			BlockPos pos, BlockPos target, Direction face) {
		return DisguiseLook.looks(view, toPos, view.getBlockState(toPos), face);
	}
}
