package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.RedstoneWireFamily;
import net.calibermc.secretblocks.blocks.SecretRepeater;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla redstone wire and Secret Redstone Dust behave as one wire family.
 * Each {@link RedStoneWireBlock} instance (a) switches off only its own
 * {@code shouldSignal} while reading the power around it, and (b) recognises
 * only its own block as wire (also in its power evaluator: see
 * {@link RedstoneWireEvaluatorFamilyMixin}). With two instances
 * each read the other as a full-strength source: no loss per block, and a
 * line latches on after its source is removed. This makes the "quiet while
 * computing" flag family-wide and treats any wire block as wire, also where
 * vanilla names {@code Blocks.REDSTONE_WIRE} to decide connections. For
 * vanilla-only circuits nothing changes (all vanilla wire is one instance).
 * {@code DiodeBlock} also names vanilla wire, but only as a fallback that
 * gives the same value as the wire's own signal, so it needs no change.
 */
@Mixin(RedStoneWireBlock.class)
public abstract class RedStoneWireFamilyMixin {

	@Shadow
	private boolean shouldSignal;

	/** The two writes around {@code getBestNeighborSignal}: quiet the whole family, not just this block. */
	@Redirect(method = "getBlockSignal",
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;shouldSignal:Z", opcode = Opcodes.PUTFIELD))
	private void secretblocks$quietTheFamily(RedStoneWireBlock self, boolean value) {
		shouldSignal = value;
		RedstoneWireFamily.signalling = value;
	}

	/** A wire signals only when neither it nor any other wire is computing its power. */
	@Redirect(method = {"getSignal", "getDirectSignal", "isSignalSource"},
			at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/RedStoneWireBlock;shouldSignal:Z", opcode = Opcodes.GETFIELD))
	private boolean secretblocks$familySignalling(RedStoneWireBlock self) {
		return shouldSignal && RedstoneWireFamily.signalling;
	}

	/** "Is this block me?" becomes "is this any wire block?" where vanilla reads or updates neighbouring wires. */
	@Redirect(method = {"checkCornerChangeAt", "updateIndirectNeighbourShapes"},
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
	private boolean secretblocks$isAnyWire(BlockState state, Block block) {
		if (block == (Object) this) {
			return RedstoneWireFamily.isWire(state);
		}
		return state.is(block);
	}

	/**
	 * (Forge and NeoForge decide connections through their block hook
	 * instead, answered by the Secret blocks' {@code canConnectRedstone}; this
	 * redirect then does nothing.)
	 * Vanilla asks "is this vanilla redstone wire?" when deciding which way a
	 * wire points, including up and down a step; any wire block counts.
	 * It also asks "is this a repeater?": a wire points at a repeater only
	 * along its axis. The Secret Repeater counts as one;
	 * otherwise, as a signal source, it would draw an arm from wire at its
	 * sides too, which a vanilla repeater does not and which would show
	 * where it is.
	 */
	@Redirect(method = "shouldConnectTo(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
	private static boolean secretblocks$connectsToAnyWire(BlockState state, Block block) {
		if (block == Blocks.REDSTONE_WIRE) {
			return RedstoneWireFamily.isWire(state);
		}
		if (block == Blocks.REPEATER) {
			return state.is(block) || state.getBlock() instanceof SecretRepeater;
		}
		return state.is(block);
	}
}
