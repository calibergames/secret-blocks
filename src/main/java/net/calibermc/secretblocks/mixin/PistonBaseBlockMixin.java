package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.SecretPiston;
import net.calibermc.secretblocks.blocks.SecretPushing;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.PistonCarry;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla's piston code, taught about the Secret Piston:
 * <ul>
 * <li>a Secret Piston builds (and removes, and updates around) its own head,
 * {@code secret_piston_head}, where vanilla names {@code piston_head};</li>
 * <li>a Secret Piston can be pushed and pulled as a vanilla piston can (only
 * retracted), and the secret blocks {@link SecretPushing} names as any block,
 * though they have a block entity;</li>
 * <li>a secret block moved by any piston carries its block entity data (the disguise, and any stuck items) in the moving
 * block ({@link PistonCarry}), including a Secret Piston's base while it
 * retracts (vanilla replaces the base with a moving block for the 2 ticks)
 * and the head it builds (the base's disguise).</li>
 * </ul>
 * Injects and argument changes where other mods are likely to change the
 * same code (pushability, the moving blocks); a redirect only for the read of
 * vanilla's head block.
 */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {

	/** The retracting base's disguise, read before vanilla replaces the base (per thread: client and server both run this). */
	@Unique
	private static final ThreadLocal<CompoundTag> secretblocks$retracting = new ThreadLocal<>();
	/** The level of the move in progress, for {@link #secretblocks$carryMoved}. */
	@Unique
	private static final ThreadLocal<Level> secretblocks$movingLevel = new ThreadLocal<>();

	@Redirect(method = "moveBlocks", require = 3,
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/Blocks;PISTON_HEAD:Lnet/minecraft/world/level/block/Block;"))
	private Block secretblocks$head() {
		return (Object) this instanceof SecretPiston ? ModBlocks.SECRET_PISTON_HEAD : Blocks.PISTON_HEAD;
	}

	/**
	 * Pistons may move the secret blocks {@link SecretPushing} names, by
	 * vanilla's rules but for its refusal of blocks with a block entity (their
	 * data moves with them), and a Secret Piston as vanilla's: inside the world,
	 * not off its edge, only retracted. (An inject, not a
	 * redirect, so mods that also change this method still load.)
	 */
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private static void secretblocks$secretPushable(BlockState state, Level level, BlockPos pos, Direction direction,
			boolean allowDestroy, Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
		boolean piston = state.getBlock() instanceof SecretPiston;
		if (!piston && !SecretPushing.isPushable(state.getBlock())) {
			return;
		}
		boolean inWorld = pos.getY() >= level.getMinBuildHeight() && pos.getY() <= level.getMaxBuildHeight() - 1
				&& level.getWorldBorder().isWithinBounds(pos);
		boolean offTheEdge = (direction == Direction.DOWN && pos.getY() == level.getMinBuildHeight())
				|| (direction == Direction.UP && pos.getY() == level.getMaxBuildHeight() - 1);
		if (!inWorld || offTheEdge) {
			cir.setReturnValue(false);
		} else if (piston) {
			cir.setReturnValue(!state.getValue(PistonBaseBlock.EXTENDED));
		} else if (state.getDestroySpeed(level, pos) == -1.0F) {
			// Vanilla's guards, kept although no pushable secret block is unbreakable or reacts other than NORMAL today.
			cir.setReturnValue(false);
		} else {
			// Vanilla's push reactions, without its refusal of blocks with a block entity.
			cir.setReturnValue(switch (state.getPistonPushReaction()) {
				case BLOCK -> false;
				case DESTROY -> allowDestroy;
				case PUSH_ONLY -> direction == pistonFacing;
				default -> true;
			});
		}
	}

	@Inject(method = "moveBlocks", at = @At("HEAD"))
	private void secretblocks$beginMove(Level level, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir) {
		secretblocks$movingLevel.set(level);
	}

	@Inject(method = "moveBlocks", at = @At("RETURN"))
	private void secretblocks$endMove(Level level, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir) {
		secretblocks$movingLevel.remove();
	}

	/**
	 * Every moving block made by {@code moveBlocks} (the blocks pushed or
	 * pulled, and a new head): carry the disguise of the secret block that is
	 * still at its source, one step back along the movement. (Vanilla handles
	 * the farthest block first, so each source is still intact here.)
	 */
	@ModifyArg(method = "moveBlocks", require = 2, index = 0,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
	private BlockEntity secretblocks$carryMoved(BlockEntity entity) {
		Level level = secretblocks$movingLevel.get();
		if (level != null && entity instanceof PistonMovingBlockEntity moving) {
			BlockPos source = moving.getBlockPos().relative(moving.getMovementDirection().getOpposite());
			if (level.getBlockEntity(source) instanceof DisguisedBlockEntity secret) {
				((PistonCarry) moving).secretblocks$setCarried(((BlockEntity) secret).saveWithoutMetadata());
			}
		}
		return entity;
	}

	/** A retracting base: keep its disguise before vanilla replaces it with a moving block (which drops its block entity). */
	@Inject(method = "triggerEvent", at = @At("HEAD"))
	private void secretblocks$keepRetracting(BlockState state, Level level, BlockPos pos, int type, int data, CallbackInfoReturnable<Boolean> cir) {
		secretblocks$retracting.remove();
		if ((type == PistonBaseBlock.TRIGGER_CONTRACT || type == PistonBaseBlock.TRIGGER_DROP)
				&& level.getBlockEntity(pos) instanceof DisguisedBlockEntity secret) {
			secretblocks$retracting.set(((BlockEntity) secret).saveWithoutMetadata());
		}
	}

	@Inject(method = "triggerEvent", at = @At("RETURN"))
	private void secretblocks$forgetRetracting(BlockState state, Level level, BlockPos pos, int type, int data, CallbackInfoReturnable<Boolean> cir) {
		secretblocks$retracting.remove();
	}

	@ModifyArg(method = "triggerEvent", require = 1, index = 0,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
	private BlockEntity secretblocks$carryRetracting(BlockEntity entity) {
		CompoundTag kept = secretblocks$retracting.get();
		if (kept != null && entity instanceof PistonCarry moving) {
			moving.secretblocks$setCarried(kept);
		}
		return entity;
	}
}
