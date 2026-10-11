package net.calibermc.secretblocks.gametest.neoforge;

import com.google.gson.JsonObject;
import net.calibermc.secretblocks.gametest.support.GameTestPlatform;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** {@link GameTestPlatform} on NeoForge and Forge 1.20.1. */
public class NeoForgeGameTestPlatform implements GameTestPlatform {

	@Override
	public InteractionResult useBlockHooks(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(player, hand, hit.getBlockPos(), hit);
		return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
	}

	@Override
	public boolean conditionsPass(JsonObject json) {
		return CraftingHelper.processConditions(json, "conditions", ICondition.IContext.EMPTY);
	}

	/** The build leaves the parked files out of this jar. */
	@Override
	public boolean shipsParkedData() {
		return false;
	}
}
