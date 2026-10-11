package net.calibermc.secretblocks.gametest.support;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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

	/** The block's appearance on {@code side} as this loader's API reports it (what connected-texture mods ask). */
	BlockState appearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side);
}
