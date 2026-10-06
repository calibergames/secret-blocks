package net.calibermc.secretblocks.blocks;

import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared state that makes vanilla redstone wire, Secret Redstone Dust and
 * Secret Redstone Wire one wire family (used by
 * {@code mixin/RedStoneWireFamilyMixin}).
 *
 * While a wire computes its power it stops all wires from signalling, so it
 * reads other wires by their power level (minus 1), never as full-strength
 * sources. Vanilla keeps that flag on the block instance, which every vanilla
 * wire shares; Secret Redstone Dust is a second instance and the Wire another
 * block, so the flag has to be shared across the family. Same thread semantics as vanilla's field (the
 * server thread computes wire power).
 */
public final class RedstoneWireFamily {

	/**
	 * False while any wire block computes its power. Public because the
	 * mixins live in another package; only the family's power calculations
	 * (the mixin and {@link SecretRedstoneWire}) write it.
	 */
	public static boolean signalling = true;

	/** Is this any wire block (vanilla redstone wire, Secret Redstone Dust or Secret Redstone Wire)? */
	public static boolean isWire(BlockState state) {
		return state.getBlock() instanceof RedStoneWireBlock || state.getBlock() instanceof SecretRedstoneWire;
	}

	private RedstoneWireFamily() {
	}
}
