package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.RedstoneWireFamily;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla redstone wire and Secret Redstone Dust behave as one wire family.
 * Each {@link RedstoneWireBlock} instance (a) switches off only its own
 * {@code shouldSignal} while reading the power around it, and (b) recognises
 * only its own block as wire (also in its power evaluator: see
 * {@link RedstoneWireEvaluatorFamilyMixin}). With two instances
 * each read the other as a full-strength source: no loss per block, and a
 * line latches on after its source is removed. This makes the "quiet while
 * computing" flag family-wide and treats any wire block as wire (which way
 * wire points is each block's own answer: see
 * {@code SecretRedstoneWire.shouldRedstoneWireConnectTo}). For
 * vanilla-only circuits nothing changes (all vanilla wire is one instance).
 * {@code DiodeBlock} also names vanilla wire, but only as a fallback that
 * gives the same value as the wire's own signal, so it needs no change.
 */
@Mixin(RedstoneWireBlock.class)
public abstract class RedStoneWireFamilyMixin {

	@Shadow
	private boolean shouldSignal;

	/** The two writes around {@code getBestNeighborSignal}: quiet the whole family, not just this block. */
	@Redirect(method = "getBlockSignal",
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/RedstoneWireBlock;shouldSignal:Z", opcode = Opcodes.PUTFIELD))
	private void secretblocks$quietTheFamily(RedstoneWireBlock self, boolean value) {
		shouldSignal = value;
		RedstoneWireFamily.signalling = value;
	}

	/** A wire signals only when neither it nor any other wire is computing its power. */
	@Redirect(method = {"getSignal", "getDirectSignal", "isSignalSource"},
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/RedstoneWireBlock;shouldSignal:Z", opcode = Opcodes.GETFIELD))
	private boolean secretblocks$familySignalling(RedstoneWireBlock self) {
		return shouldSignal && RedstoneWireFamily.signalling;
	}

	/** "Is this block me?" becomes "is this any wire block?" where vanilla reads or updates neighbouring wires. */
	@Redirect(method = {"checkCornerChangeAt", "updateIndirectNeighbourShapes"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Ljava/lang/Object;)Z"))
	private boolean secretblocks$isAnyWire(BlockState state, Object block) {
		if (block == (Object) this) {
			return RedstoneWireFamily.isWire(state);
		}
		return state.is((Block) block);
	}
}
