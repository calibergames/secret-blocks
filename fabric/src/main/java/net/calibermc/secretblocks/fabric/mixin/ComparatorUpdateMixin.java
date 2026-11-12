package net.calibermc.secretblocks.fabric.mixin;

import net.calibermc.secretblocks.blocks.SecretComparator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When a container's contents change (or an item frame's item or rotation),
 * {@code Level.updateNeighbourForOutputSignal} tells the comparators beside
 * it, and those one solid block further on. In 1.20.1 it asks "is this
 * {@code Blocks.COMPARATOR}?", so a Secret Comparator would never hear of a
 * change and would keep its old output. Both questions also accept the
 * Secret Comparator. Nothing changes for vanilla comparators. (Forge and
 * NeoForge ask the blocks themselves instead; the Secret Comparator answers
 * there through its own {@code getWeakChanges}.)
 */
@Mixin(Level.class)
public abstract class ComparatorUpdateMixin {

	// Both questions must be redirected: the comparator beside the container and the one a solid block further on.
	@Redirect(method = "updateNeighbourForOutputSignal", require = 2,
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
	private boolean secretblocks$isAnyComparator(BlockState state, Object block) {
		return state.is((Block) block) || (block == Blocks.COMPARATOR && state.getBlock() instanceof SecretComparator);
	}
}
