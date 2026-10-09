package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.calibermc.secretblocks.blocks.SecretBlock;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A secret block a piston breaks bursts into its look's particles, not
 * stone's. The burst is shown after the block (and its block entity) is
 * gone, so the look of its top is read while it is dropped, just before.
 */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBreakLookMixin {

	/**
	 * The look of the block being broken, between its drop and its burst
	 * (per thread: client and server both move pistons). Set at every drop
	 * and cleared at every burst, so nothing stale reaches another block: a
	 * block with no burst (fire) is never a secret block, which alone sets one.
	 */
	@Unique
	private static final ThreadLocal<BlockState> secretblocks$breakingLook = new ThreadLocal<>();

	@ModifyArg(method = "moveBlocks", index = 3,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
	@Nullable
	private BlockEntity secretblocks$readLook(@Nullable BlockEntity entity) {
		secretblocks$breakingLook.set(entity instanceof DisguisedBlockEntity secret && entity.getLevel() != null
				? DisguiseLook.shown(secret, entity.getLevel(), entity.getBlockPos(), Direction.UP) : null);
		return entity;
	}

	@ModifyArg(method = "moveBlocks", index = 1,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addDestroyBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
	private BlockState secretblocks$burstLook(BlockState state) {
		BlockState look = secretblocks$breakingLook.get();
		secretblocks$breakingLook.remove();
		return look != null && state.getBlock() instanceof SecretBlock ? look : state;
	}
}
