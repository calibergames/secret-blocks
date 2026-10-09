package net.calibermc.secretblocks.gametest.support.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Sets a test player's game mode without the packets a real change sends
 * (test players have no connection). Game tests only.
 */
@Mixin(ServerPlayerGameMode.class)
public interface ServerPlayerGameModeAccessor {

	@Accessor("gameModeForPlayer")
	void secretblocksTest$setGameModeForPlayer(GameType mode);
}
