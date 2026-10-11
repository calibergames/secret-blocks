package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Footsteps on a secret block and the dust of sprinting over it follow the look of its top. */
@Mixin(Entity.class)
public abstract class EntityStepLookMixin {

	@Shadow
	public Level level;

	@Shadow
	public abstract double getX();

	@Shadow
	public abstract double getY();

	@Shadow
	public abstract double getZ();

	@ModifyVariable(method = "playStepSound", at = @At("HEAD"), argsOnly = true)
	private BlockState secretblocks$stepLook(BlockState state, BlockPos pos) {
		return DisguiseLook.looks(level, pos, state, Direction.UP);
	}

	/** Vanilla reads the block under the entity's feet here (the same position). */
	@ModifyVariable(method = "spawnSprintParticle", at = @At("STORE"))
	private BlockState secretblocks$sprintLook(BlockState state) {
		BlockPos pos = new BlockPos(Mth.floor(getX()), Mth.floor(getY() - 0.2F), Mth.floor(getZ()));
		return DisguiseLook.looks(level, pos, state, Direction.UP);
	}
}
