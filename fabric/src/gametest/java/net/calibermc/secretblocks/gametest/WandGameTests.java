package net.calibermc.secretblocks.gametest;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Camouflage Wand, loading: sneak + use loads a block (or a secret block's
 * clicked face) as the wand's disguise, through the use-block hook that runs
 * before the block's own use. Creative or operator only.
 */
public class WandGameTests implements FabricGameTest {

	private static final BlockPos TARGET = new BlockPos(3, 1, 3);
	private static final Vec3 FEET_NORTH = new Vec3(3.5, 1, 0.5);

	private static WandTestPlayer sneaking(GameTestHelper h, boolean creative, int permission) {
		return WandTestPlayer.at(h, FEET_NORTH, creative, permission, true);
	}

	private static InteractionResult click(GameTestHelper h, WandTestPlayer player, ItemStack stack) {
		return player.click(h, stack, TARGET, Direction.NORTH, true);
	}

	private static BlockHitResult hit(GameTestHelper h) {
		return new BlockHitResult(abs(h, new Vec3(3.5, 1.5, 3.0)), Direction.NORTH, h.absolutePos(TARGET), false);
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
		WandTestPlayer player = sneaking(h, true, 0);
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
		click(h, sneaking(h, true, 0), wand);
		check(h, "minecraft:emerald_block".equals(loadedName(wand)), "the wand should carry the north face, got " + wand.getTag());
		h.succeed();
	}

	/**
	 * The hook is registered and takes the click on a lever. (Sneaking with an
	 * item already keeps vanilla from using the block; plain use, where the
	 * hook going first matters, is tested with the conversions.)
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theRegisteredHookTakesTheClick(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.LEVER.defaultBlockState());
		WandTestPlayer player = sneaking(h, true, 0);
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, wand);
		// The registered hook itself, as the loader calls it.
		InteractionResult result = UseBlockCallback.EVENT.invoker().interact(player, h.getLevel(), InteractionHand.MAIN_HAND, hit(h));
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
		WandTestPlayer player = sneaking(h, false, 0);
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
		click(h, sneaking(h, false, 2), wand);
		check(h, "minecraft:polished_granite".equals(loadedName(wand)), "an operator should load, got " + wand.getTag());
		h.succeed();
	}

	/** A secret face that shows no block has nothing to load. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBlankSecretFaceIsRefused(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, ModBlocks.SOLID_BLOCK);
		secret(h, TARGET).setState(Direction.NORTH, Blocks.AIR.defaultBlockState());
		WandTestPlayer player = sneaking(h, true, 0);
		ItemStack wand = new ItemStack(ModItems.CAMOUFLAGE_WAND);
		wand.getOrCreateTag().put(CamouflageWand.DISGUISE, NbtUtils.writeBlockState(Blocks.GOLD_BLOCK.defaultBlockState()));
		InteractionResult result = click(h, player, wand);
		check(h, result == InteractionResult.FAIL, "the click should fail, got " + result);
		check(h, "minecraft:gold_block".equals(loadedName(wand)), "the wand should keep what it carried, got " + wand.getTag());
		check(h, player.lastMessageKey().endsWith(".nothing_to_load"), "the player should be told why, got " + player.messages);
		h.succeed();
	}

	/** With another item, or for a spectator, the hook stays out of the way. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theHookPassesOtherwise(GameTestHelper h) {
		floor(h);
		h.setBlock(TARGET, Blocks.POLISHED_GRANITE);
		WandTestPlayer other = sneaking(h, true, 0);
		other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SWITCH_PROBE));
		check(h, CamouflageWand.onUseBlock(other, h.getLevel(), InteractionHand.MAIN_HAND, hit(h)) == InteractionResult.PASS,
				"another item should pass");
		WandTestPlayer spectator = sneaking(h, true, 0);
		spectator.spectator = true;
		spectator.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CAMOUFLAGE_WAND));
		check(h, CamouflageWand.onUseBlock(spectator, h.getLevel(), InteractionHand.MAIN_HAND, hit(h)) == InteractionResult.PASS,
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
		CompoundTag numeric = new CompoundTag();
		numeric.putInt("Name", 3);
		wand.getOrCreateTag().put(CamouflageWand.DISGUISE, numeric);
		check(h, CamouflageWand.loadedDisguise(wand).isEmpty(), "a numeric id should read as empty");
		h.succeed();
	}
}
