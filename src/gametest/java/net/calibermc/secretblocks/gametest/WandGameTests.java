package net.calibermc.secretblocks.gametest;

import com.mojang.authlib.GameProfile;
import net.calibermc.secretblocks.items.CamouflageWand;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Camouflage Wand, loading: sneak + use loads a block (or a secret block's
 * clicked face) as the wand's disguise, through the use-block hook that runs
 * before the block's own use. Creative or operator only.
 */
public class WandGameTests implements FabricGameTest {

	private static final GameProfile PROFILE = new GameProfile(
			UUID.nameUUIDFromBytes("secretblocks-gametest-wand".getBytes(StandardCharsets.UTF_8)), "sb_wand");

	private static final BlockPos TARGET = new BlockPos(3, 1, 3);
	private static final Vec3 TARGET_NORTH = new Vec3(3.5, 1.5, 3.0);
	private static final Vec3 FEET_NORTH = new Vec3(3.5, 1, 0.5);

	/** A test player whose creative mode and permission level are set by the test, and whose messages are kept. */
	private static final class WandPlayer extends ServerPlayer {
		final boolean creative;
		final int permission;
		boolean spectator;
		final List<Component> messages = new ArrayList<>();

		WandPlayer(ServerLevel level, boolean creative, int permission) {
			super(level.getServer(), level, PROFILE, null);
			this.creative = creative;
			this.permission = permission;
		}

		@Override
		public boolean isCreative() {
			return creative;
		}

		@Override
		public boolean isSpectator() {
			return spectator;
		}

		@Override
		protected int getPermissionLevel() {
			return permission;
		}

		@Override
		public void displayClientMessage(Component message, boolean actionBar) {
			messages.add(message);
		}

		String lastMessageKey() {
			if (messages.isEmpty()) {
				return "";
			}
			return messages.get(messages.size() - 1).getContents() instanceof TranslatableContents t ? t.getKey() : "";
		}
	}

	private static WandPlayer wandPlayer(GameTestHelper h, boolean creative, int permission) {
		WandPlayer player = new WandPlayer(h.getLevel(), creative, permission);
		Vec3 feet = abs(h, FEET_NORTH);
		player.moveTo(feet.x, feet.y, feet.z, 0, 0);
		player.setShiftKeyDown(true);
		return player;
	}

	/**
	 * A click on the north face of {@link #TARGET} through the server's real
	 * use path ({@code ServerPlayerGameMode.useItemOn}), where the hook runs
	 * before the block's own use.
	 */
	private static InteractionResult click(GameTestHelper h, ServerPlayer player, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hit = new BlockHitResult(abs(h, TARGET_NORTH), Direction.NORTH, h.absolutePos(TARGET), false);
		return player.gameMode.useItemOn(player, h.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
	}

	private static String loadedName(ItemStack wand) {
		CompoundTag tag = wand.getTag();
		return tag == null ? "" : tag.getCompound(CamouflageWand.DISGUISE).getString("Name");
	}

	/** Sneak + use on a plain block loads it, state included. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void sneakUseLoadsABlock(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.POLISHED_GRANITE);
		WandPlayer player = wandPlayer(h, true, 0);
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		InteractionResult result = click(h, player, wand);
		check(h, result.consumesAction(), "the load should consume the click, got " + result);
		check(h, "minecraft:polished_granite".equals(loadedName(wand)), "the wand should carry polished granite, got " + wand.getTag());
		check(h, CamouflageWand.loadedDisguise(wand).map(s -> s.is(Blocks.POLISHED_GRANITE)).orElse(false),
				"the stored disguise should read back");
		check(h, player.lastMessageKey().endsWith(".loaded"), "the player should be told what was loaded, got " + player.messages);
		h.succeed();
	}

	/** On a secret block the wand loads the face that was clicked, not the secret block. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void sneakUseOnASecretBlockLoadsTheClickedFace(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, ModBlocks.SOLID_BLOCK);
		secret(h, TARGET).setState(Blocks.STONE.defaultBlockState());
		secret(h, TARGET).setState(Direction.NORTH, Blocks.EMERALD_BLOCK.defaultBlockState());
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		click(h, wandPlayer(h, true, 0), wand);
		check(h, "minecraft:emerald_block".equals(loadedName(wand)), "the wand should carry the north face, got " + wand.getTag());
		h.succeed();
	}

	/**
	 * The hook is registered and takes the click on a lever. (Sneaking with an
	 * item already keeps vanilla from using the block; the hook going first
	 * matters for plain use.)
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theRegisteredHookTakesTheClick(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET.below(), Blocks.STONE);
		BlockState lever = Blocks.LEVER.defaultBlockState();
		h.setBlock(TARGET, lever);
		WandPlayer player = wandPlayer(h, true, 0);
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, wand);
		BlockHitResult hit = new BlockHitResult(abs(h, TARGET_NORTH), Direction.NORTH, h.absolutePos(TARGET), false);
		// The registered hook itself, as the loader calls it.
		InteractionResult result = UseBlockCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, hit);
		check(h, result == InteractionResult.SUCCESS, "the registered hook should take the click, got " + result);
		check(h, "minecraft:lever".equals(loadedName(wand)), "the wand should carry the lever, got " + wand.getTag());
		check(h, !h.getBlockState(TARGET).getValue(LeverBlock.POWERED), "the lever must not flip");
		h.succeed();
	}

	/** A survival player who is not an operator is refused and nothing changes. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSurvivalNonOperatorIsRefused(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.POLISHED_GRANITE);
		WandPlayer player = wandPlayer(h, false, 0);
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		InteractionResult result = click(h, player, wand);
		check(h, result == InteractionResult.FAIL, "the click should fail, got " + result);
		check(h, !wand.hasTag(), "the wand should stay empty, got " + wand.getTag());
		check(h, player.lastMessageKey().endsWith(".not_allowed"), "the player should be told why, got " + player.messages);
		h.succeed();
	}

	/** An operator (permission level 2) may use it in survival. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void anOperatorInSurvivalMayUseIt(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.POLISHED_GRANITE);
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		click(h, wandPlayer(h, false, 2), wand);
		check(h, "minecraft:polished_granite".equals(loadedName(wand)), "an operator should load, got " + wand.getTag());
		h.succeed();
	}

	/** A secret face that shows no block has nothing to load. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBlankSecretFaceIsRefused(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, ModBlocks.SOLID_BLOCK);
		secret(h, TARGET).setState(Direction.NORTH, Blocks.AIR.defaultBlockState());
		WandPlayer player = wandPlayer(h, true, 0);
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		wand.getOrCreateTag().put(CamouflageWand.DISGUISE, NbtUtils.writeBlockState(Blocks.GOLD_BLOCK.defaultBlockState()));
		InteractionResult result = click(h, player, wand);
		check(h, result == InteractionResult.FAIL, "the click should fail, got " + result);
		check(h, "minecraft:gold_block".equals(loadedName(wand)), "the wand should keep what it carried, got " + wand.getTag());
		check(h, player.lastMessageKey().endsWith(".nothing_to_load"), "the player should be told why, got " + player.messages);
		h.succeed();
	}

	/** Without sneaking, or with another item, the hook stays out of the way. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theHookPassesOtherwise(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.POLISHED_GRANITE);
		BlockHitResult hit = new BlockHitResult(abs(h, TARGET_NORTH), Direction.NORTH, h.absolutePos(TARGET), false);
		WandPlayer standing = wandPlayer(h, true, 0);
		standing.setShiftKeyDown(false);
		standing.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAMOUFLAGE_WAND));
		check(h, CamouflageWand.onUseBlock(standing, h.getLevel(), InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS,
				"plain use is not the wand's yet");
		WandPlayer other = wandPlayer(h, true, 0);
		other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SWITCH_PROBE));
		check(h, CamouflageWand.onUseBlock(other, h.getLevel(), InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS,
				"another item should pass");
		WandPlayer spectator = wandPlayer(h, true, 0);
		spectator.spectator = true;
		spectator.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAMOUFLAGE_WAND));
		check(h, CamouflageWand.onUseBlock(spectator, h.getLevel(), InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS,
				"a spectator should pass");
		h.succeed();
	}

	/** The stored disguise is checked again when read: unknown blocks, air and secret blocks read as empty. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aTamperedWandReadsAsEmpty(GameTestHelper h) {
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		check(h, CamouflageWand.loadedDisguise(wand).isEmpty(), "a new wand is empty");
		CompoundTag unknown = new CompoundTag();
		unknown.putString("Name", "nosuchmod:nosuchblock");
		wand.getOrCreateTag().put(CamouflageWand.DISGUISE, unknown);
		check(h, CamouflageWand.loadedDisguise(wand).isEmpty(), "an unknown block should read as empty");
		wand.getOrCreateTag().put(CamouflageWand.DISGUISE, NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));
		check(h, CamouflageWand.loadedDisguise(wand).isEmpty(), "air should read as empty");
		wand.getOrCreateTag().put(CamouflageWand.DISGUISE, NbtUtils.writeBlockState(ModBlocks.SOLID_BLOCK.defaultBlockState()));
		check(h, CamouflageWand.loadedDisguise(wand).isEmpty(), "a secret block should read as empty");
		wand.getOrCreateTag().putString(CamouflageWand.DISGUISE, "minecraft:stone");
		check(h, CamouflageWand.loadedDisguise(wand).isEmpty(), "a value of the wrong type should read as empty");
		for (String bad : new String[] {"Bad Name", "Foo:Bar", "minecraft:stone:x"}) {
			CompoundTag malformed = new CompoundTag();
			malformed.putString("Name", bad);
			wand.getOrCreateTag().put(CamouflageWand.DISGUISE, malformed);
			check(h, CamouflageWand.loadedDisguise(wand).isEmpty(), "a malformed id should read as empty: " + bad);
		}
		CompoundTag noName = new CompoundTag();
		noName.putInt("Name", 3);
		wand.getOrCreateTag().put(CamouflageWand.DISGUISE, noName);
		check(h, CamouflageWand.loadedDisguise(wand).isEmpty(), "a numeric id should read as empty");
		h.succeed();
	}
}
