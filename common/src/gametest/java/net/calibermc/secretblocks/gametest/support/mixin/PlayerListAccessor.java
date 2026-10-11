package net.calibermc.secretblocks.gametest.support.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * The server's own list of connected players: {@code getPlayers()} returns a
 * read-only view on some loaders, and the wand tests list a test player there
 * for the length of one click.
 */
@Mixin(PlayerList.class)
public interface PlayerListAccessor {

	@Accessor("players")
	List<ServerPlayer> secretblocksTest$getPlayers();
}
