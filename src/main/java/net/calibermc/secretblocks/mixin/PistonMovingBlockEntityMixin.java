package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.SecretPiston;
import net.calibermc.secretblocks.blocks.SecretPistonHead;
import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.PistonCarry;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The moving block carries a secret block's block entity data ({@link
 * PistonCarry}): saved with it (a world saved mid-move keeps it; clients
 * build their own moving blocks from the same block event), and given to the
 * block placed when the move ends. A Secret Piston's moving head collides as
 * its full-block head, not as vanilla's thin one.
 */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonMovingBlockEntityMixin implements PistonCarry {

	/** The key of the carried data in the moving block's saved data. Its name dates from an earlier development build that carried only the disguise; kept so worlds saved mid-move by it still load. */
	@Unique
	private static final String SECRETBLOCKS_CARRIED = "SecretBlocksDisguise";

	@Shadow
	private BlockState movedState;

	@Unique
	@Nullable
	private CompoundTag secretblocks$carried;
	@Unique
	@Nullable
	private Disguise secretblocks$disguise;

	@Override
	@Nullable
	public CompoundTag secretblocks$getCarried() {
		return secretblocks$carried;
	}

	@Override
	public void secretblocks$setCarried(@Nullable CompoundTag disguise) {
		secretblocks$carried = disguise;
		secretblocks$disguise = null;
	}

	@Override
	@Nullable
	public Disguise secretblocks$getCarriedDisguise() {
		if (secretblocks$disguise == null && secretblocks$carried != null) {
			Disguise disguise = new Disguise();
			disguise.load(secretblocks$carried);
			secretblocks$disguise = disguise;
		}
		return secretblocks$disguise;
	}

	/**
	 * A moving block made during a piston move ({@link PistonCarry#MOVING_LEVEL})
	 * carries the data of the secret block still at its source, one step back
	 * along the movement (vanilla moves the farthest block first, so each
	 * source is still intact here). Taken when the moving block is made, not
	 * where vanilla places it: Carpet makes its own moving blocks in place of
	 * vanilla's.
	 */
	@Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;ZZ)V",
			at = @At("TAIL"), require = 1)
	private void secretblocks$carryFromSource(CallbackInfo ci) {
		Level level = PistonCarry.MOVING_LEVEL.get();
		if (level == null) {
			return;
		}
		PistonMovingBlockEntity self = (PistonMovingBlockEntity) (Object) this;
		BlockPos source = self.getBlockPos().relative(self.getMovementDirection().getOpposite());
		if (level.getBlockEntity(source) instanceof DisguisedBlockEntity secret) {
			secretblocks$setCarried(((BlockEntity) secret).saveWithoutMetadata());
		}
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void secretblocks$save(CompoundTag tag, CallbackInfo ci) {
		if (secretblocks$carried != null) {
			tag.put(SECRETBLOCKS_CARRIED, secretblocks$carried.copy());
		}
	}

	/**
	 * Clients get the moving block's data (vanilla sends it with the chunk) but
	 * only the disguise of what it carries, as a secret block's own update sends
	 * only its disguise: never items stuck in it.
	 */
	@Inject(method = "getUpdateTag", at = @At("RETURN"))
	private void secretblocks$disguiseOnlyToClients(CallbackInfoReturnable<CompoundTag> cir) {
		CompoundTag tag = cir.getReturnValue();
		Disguise disguise = secretblocks$getCarriedDisguise();
		if (disguise != null) {
			tag.put(SECRETBLOCKS_CARRIED, disguise.saveForClients(new CompoundTag()));
		}
	}

	@Inject(method = "load", at = @At("TAIL"))
	private void secretblocks$load(CompoundTag tag, CallbackInfo ci) {
		secretblocks$setCarried(tag.contains(SECRETBLOCKS_CARRIED, CompoundTag.TAG_COMPOUND) ? tag.getCompound(SECRETBLOCKS_CARRIED) : null);
	}

	/** The block placed at the end of the move gets the carried block entity data (disguise, stuck items). */
	@Unique
	private static void secretblocks$restore(Level level, BlockPos pos, PistonMovingBlockEntity moving) {
		CompoundTag carried = ((PistonCarry) moving).secretblocks$getCarried();
		if (carried != null && level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret) {
			((BlockEntity) secret).load(carried);
			secret.markDisguiseChanged();
		}
	}

	@Inject(method = "tick", require = 2,
			at = @At(value = "INVOKE", shift = At.Shift.AFTER,
					target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private static void secretblocks$restoreAfterMove(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity moving, CallbackInfo ci) {
		secretblocks$restore(level, pos, moving);
	}

	@Inject(method = "finalTick", require = 1,
			at = @At(value = "INVOKE", shift = At.Shift.AFTER,
					target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
	private void secretblocks$restoreAfterFinalTick(CallbackInfo ci) {
		PistonMovingBlockEntity self = (PistonMovingBlockEntity) (Object) this;
		secretblocks$restore(self.getLevel(), self.getBlockPos(), self);
	}

	/** A Secret Piston's head collides as itself (a full block) while it moves. */
	@Redirect(method = {"getCollisionRelatedBlockState", "getCollisionShape"}, require = 2,
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
	private Block secretblocks$movingHead() {
		Block moved = movedState.getBlock();
		return moved instanceof SecretPiston || moved instanceof SecretPistonHead ? ModBlocks.SECRET_PISTON_HEAD : Blocks.PISTON_HEAD;
	}
}
