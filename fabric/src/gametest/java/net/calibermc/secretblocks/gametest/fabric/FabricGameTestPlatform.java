package net.calibermc.secretblocks.gametest.fabric;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.calibermc.secretblocks.gametest.support.GameTestPlatform;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;

/** {@link GameTestPlatform} on Fabric. */
public class FabricGameTestPlatform implements GameTestPlatform {

	@Override
	public InteractionResult useBlockHooks(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		return UseBlockCallback.EVENT.invoker().interact(player, level, hand, hit);
	}

	@Override
	public boolean conditionsPass(JsonObject json) {
		JsonElement conditions = json.get(ResourceConditions.CONDITIONS_KEY);
		// The mod's own files use no condition that needs the registries (tag conditions do).
		return conditions == null || ResourceCondition.CONDITION_CODEC.parse(JsonOps.INSTANCE, conditions).getOrThrow().test(null);
	}

	@Override
	public BlockState appearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side) {
		return state.getAppearance(level, pos, side, null, null);
	}

	@Override
	public void chunkLoaded(ServerLevel level, LevelChunk chunk) {
		ServerChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(level, chunk);
	}
}
