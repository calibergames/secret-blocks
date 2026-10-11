package net.calibermc.secretblocks.blocks.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Added to vanilla's {@link PistonMovingBlockEntity} ({@code
 * PistonMovingBlockEntityMixin}): the block entity data of a secret block
 * while a piston moves it: its disguise, and any items
 * stuck in it before 2.0.0. Vanilla never moves block entities, so a moved
 * secret block would lose them; the move carries them here, saves them with
 * the moving block, and gives them to the block placed at the end.
 */
public interface PistonCarry {

	/**
	 * The level of the piston move in progress on this thread (client and
	 * server both run moves), set for the length of {@code
	 * PistonBaseBlock.moveBlocks}: a moving block made then carries the data
	 * of the secret block still at its source.
	 */
	ThreadLocal<Level> MOVING_LEVEL = new ThreadLocal<>();

	/** The carried block entity data (without id and position), or {@code null}. */
	@Nullable
	CompoundTag secretblocks$getCarried();

	void secretblocks$setCarried(@Nullable CompoundTag data);

	/** The disguise in the carried data, read once (drawing the move every frame). */
	@Nullable
	Disguise secretblocks$getCarriedDisguise();
}
