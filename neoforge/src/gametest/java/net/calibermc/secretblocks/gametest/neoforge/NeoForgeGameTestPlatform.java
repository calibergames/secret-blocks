package net.calibermc.secretblocks.gametest.neoforge;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import net.calibermc.secretblocks.gametest.support.GameTestPlatform;
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
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** {@link GameTestPlatform} on NeoForge. */
public class NeoForgeGameTestPlatform implements GameTestPlatform {

	@Override
	public InteractionResult useBlockHooks(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		PlayerInteractEvent.RightClickBlock event = CommonHooks.onRightClickBlock(player, hand, hit.getBlockPos(), hit);
		return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
	}

	@Override
	public boolean conditionsPass(JsonObject json) {
		return ICondition.conditionsMatched(JsonOps.INSTANCE, (JsonElement) json);
	}

	@Override
	public BlockState appearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side) {
		return state.getAppearance(level, pos, side, null, null);
	}

	@Override
	public void chunkLoaded(ServerLevel level, LevelChunk chunk) {
		NeoForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false));
	}
}
