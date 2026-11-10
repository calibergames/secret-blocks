package net.calibermc.secretblocks.gametest.forge;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
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
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;

/** {@link GameTestPlatform} on Forge. */
public class ForgeGameTestPlatform implements GameTestPlatform {

	@Override
	public InteractionResult useBlockHooks(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(player, hand, hit.getBlockPos(), hit);
		return PlayerInteractEvent.RightClickBlock.BUS.post(event) ? event.getCancellationResult() : InteractionResult.PASS;
	}

	@Override
	public boolean conditionsPass(JsonObject json) {
		JsonElement condition = json.get(ICondition.DEFAULT_FIELD);
		return condition == null || ICondition.CODEC.parse(JsonOps.INSTANCE, condition).getOrThrow().test(ICondition.IContext.EMPTY, JsonOps.INSTANCE);
	}

	@Override
	public BlockState appearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side) {
		return state.getAppearance(level, pos, side, null, null);
	}

	@Override
	public void chunkLoaded(ServerLevel level, LevelChunk chunk) {
		ChunkEvent.Load.BUS.post(new ChunkEvent.Load(chunk, false));
	}
}
