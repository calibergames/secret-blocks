package net.calibermc.secretblocks.gametest.support;

import com.google.gson.JsonObject;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ServiceLoader;

/** What the game tests need from the loader they run on; each loader's test mod lists its implementation in META-INF/services. */
public interface GameTestPlatform {

	GameTestPlatform INSTANCE = ServiceLoader.load(GameTestPlatform.class, GameTestPlatform.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("No GameTestPlatform for this loader"));

	/** The loader's use-block hooks, as they run when a player uses an item on a block; PASS if none took the click. */
	InteractionResult useBlockHooks(Player player, Level level, InteractionHand hand, BlockHitResult hit);

	/** Whether this loader's load conditions in a data file (if it has any) let it load. */
	boolean conditionsPass(JsonObject json);

	/** Whether this loader's jar ships the parked data files (behind a load condition) at all. */
	boolean shipsParkedData();
}
