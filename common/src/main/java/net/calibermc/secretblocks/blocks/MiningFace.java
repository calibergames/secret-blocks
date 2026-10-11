package net.calibermc.secretblocks.blocks;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * The face of the block a player is mining, kept on the player from the
 * dig action (the client's own and the one the server receives), so the
 * mining time and sounds follow the look of that face.
 */
public interface MiningFace {

	@Nullable
	Direction secretblocks$getMiningFace();

	void secretblocks$setMiningFace(@Nullable Direction face);
}
