package net.calibermc.secretblocks.render;

import net.calibermc.secretblocks.blocks.entity.Disguise;
import net.calibermc.secretblocks.blocks.entity.PistonCarry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The moving block being drawn right now ({@code PistonHeadRendererMixin}),
 * so that a secret block drawn as part of a move wears the disguise the move
 * carries ({@link PistonCarry}) instead of looking up a block entity that is
 * not there during the move.
 */
@Environment(EnvType.CLIENT)
public final class MovingDisguise {

	private static final ThreadLocal<PistonMovingBlockEntity> CURRENT = new ThreadLocal<>();

	private MovingDisguise() {
	}

	public static void begin(PistonMovingBlockEntity moving) {
		CURRENT.set(moving);
	}

	public static void end() {
		CURRENT.remove();
	}

	@Nullable
	public static PistonMovingBlockEntity moving() {
		return CURRENT.get();
	}

	/** Whether a move is being drawn right now. */
	public static boolean isDrawing() {
		return CURRENT.get() != null;
	}

	/** The disguise of the block being drawn as part of a move, or {@code null} outside a move. */
	@Nullable
	public static Disguise current() {
		PistonMovingBlockEntity moving = CURRENT.get();
		return moving == null ? null : ((PistonCarry) moving).secretblocks$getCarriedDisguise();
	}
}
