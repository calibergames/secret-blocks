package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Landing on a secret block sounds and puffs like the look of its top. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFallLookMixin extends Entity {

	private LivingEntityFallLookMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	/** Vanilla reads the block under the entity's feet here (the same position). */
	@ModifyVariable(method = "playBlockFallSound", at = @At("STORE"))
	private BlockState secretblocks$fallSoundLook(BlockState state) {
		BlockPos pos = new BlockPos(Mth.floor(getX()), Mth.floor(getY() - 0.2F), Mth.floor(getZ()));
		return DisguiseLook.looks(level(), pos, state, Direction.UP);
	}

	/**
	 * Only the particles change: the fall itself still lands on the secret
	 * block. The block landed on is the one vanilla passes here.
	 */
	@ModifyArg(method = "checkFallDamage", index = 1,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/core/particles/BlockParticleOption;<init>(Lnet/minecraft/core/particles/ParticleType;Lnet/minecraft/world/level/block/state/BlockState;)V"))
	private BlockState secretblocks$landingLook(BlockState state) {
		return DisguiseLook.looks(level(), getOnPosLegacy(), state, Direction.UP);
	}
}
