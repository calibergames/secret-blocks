package net.calibermc.secretblocks.blocks;

import java.util.Set;

import net.calibermc.secretblocks.registry.ModBlocks;
import net.minecraft.world.level.block.Block;

/**
 * Which secret blocks pistons may push and pull. Vanilla refuses any block
 * with a block entity; these full disguised blocks move anyway, their block
 * entity data (disguise, and any items stuck in them before 2.0.0) carried by
 * the moving block ({@code PistonCarry}). The Secret Pistons are pushable
 * too, but only retracted, as vanilla's ({@code PistonBaseBlockMixin}).
 *
 * Not pushable, as in vanilla: the containers (chests, dispenser, dropper,
 * hopper), the sculk sensor and the daylight detector. The Secret Comparator
 * stays unpushable: it stops a piston, which breaks a vanilla comparator.
 * The Secret Gate and its segments stay unpushable (a moved gate would strand
 * its segments). The Secret Trapdoors move, as vanilla's do (found when the
 * game tests compared them). A piston breaks the Secret Doors (vanilla's
 * door code), Buttons, Lever and pressure plates (their own push reaction,
 * matching vanilla's), as it breaks vanilla's.
 */
public final class SecretPushing {

	private SecretPushing() {
	}

	/** Built on first use (the blocks are registered by then). */
	private static final class Holder {
		private static final Set<Block> PUSHABLE = Set.of(ModBlocks.SOLID_BLOCK, ModBlocks.GHOST_BLOCK, ModBlocks.ONE_WAY_GLASS,
				ModBlocks.STAIR_BLOCK, ModBlocks.SLAB_BLOCK, ModBlocks.SECRET_CLAMBER, ModBlocks.SECRET_REDSTONE,
				ModBlocks.SECRET_REDSTONE_WIRE, ModBlocks.SECRET_REPEATER, ModBlocks.SECRET_OBSERVER, ModBlocks.TRAPDOOR_BLOCK,
				ModBlocks.IRON_TRAPDOOR_BLOCK);
	}

	public static boolean isPushable(Block block) {
		return Holder.PUSHABLE.contains(block);
	}

	/** Every pushable secret block (the game tests push each one). */
	public static Set<Block> pushableBlocks() {
		return Holder.PUSHABLE;
	}
}
