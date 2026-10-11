package net.calibermc.secretblocks.mixin;

import net.calibermc.secretblocks.blocks.MiningFace;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Keeps the face of the block the player is mining ({@link MiningFace}). */
@Mixin(Player.class)
public abstract class PlayerMiningFaceMixin implements MiningFace {

	@Unique
	@Nullable
	private Direction secretblocks$miningFace;

	@Override
	@Nullable
	public Direction secretblocks$getMiningFace() {
		return secretblocks$miningFace;
	}

	@Override
	public void secretblocks$setMiningFace(@Nullable Direction face) {
		secretblocks$miningFace = face;
	}
}
