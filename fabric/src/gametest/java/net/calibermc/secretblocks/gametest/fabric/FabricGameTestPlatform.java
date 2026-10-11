package net.calibermc.secretblocks.gametest.fabric;

import com.google.gson.JsonObject;
import net.calibermc.secretblocks.gametest.support.GameTestPlatform;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** {@link GameTestPlatform} on Fabric. */
public class FabricGameTestPlatform implements GameTestPlatform {

	@Override
	public InteractionResult useBlockHooks(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		return UseBlockCallback.EVENT.invoker().interact(player, level, hand, hit);
	}

	@Override
	public boolean conditionsPass(JsonObject json) {
		return ResourceConditions.objectMatchesConditions(json);
	}

	@Override
	public BlockState appearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side) {
		return state.getAppearance(level, pos, side, null, null);
	}
}
