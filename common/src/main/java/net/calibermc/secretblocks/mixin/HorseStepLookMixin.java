package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.DisguiseLook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hooves on a secret block follow the look of its top (horses pick wood or other steps by it). */
@Mixin(AbstractHorse.class)
public abstract class HorseStepLookMixin extends Animal {

	private HorseStepLookMixin(EntityType<? extends Animal> type, Level level) {
		super(type, level);
	}

	@ModifyVariable(method = "playStepSound", at = @At("HEAD"), argsOnly = true)
	private BlockState secretblocks$stepLook(BlockState state, BlockPos pos) {
		return DisguiseLook.looks(level(), pos, state, Direction.UP);
	}
}
