package net.calibermc.secretblocks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's private "look at the power and extend or retract" (the Secret Piston calls it when placed). */
@Mixin(PistonBaseBlock.class)
public interface PistonBaseBlockAccessor {

	@Invoker("checkIfExtend")
	void secretblocks$checkIfExtend(Level level, BlockPos pos, BlockState state);
}
