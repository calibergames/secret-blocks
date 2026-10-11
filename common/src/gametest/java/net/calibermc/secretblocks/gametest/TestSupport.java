package net.calibermc.secretblocks.gametest;

import com.mojang.authlib.GameProfile;
import net.calibermc.secretblocks.blocks.entity.DisguisedBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.blocks.entity.SecretChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared helpers for the Secret Blocks game tests.
 *
 * Positions passed in are relative to the test structure (as
 * {@link GameTestHelper} uses them). Placement goes through the real item
 * path ({@link ItemStack#useOn}) with a real {@link ServerPlayer}: the 1.2.0
 * placement code casts the placer to {@code ServerPlayer} and aims a ray from
 * the player's eye, so the mock player from {@code makeMockPlayer()} (a plain
 * {@code Player}) cannot be used for placement.
 */
final class TestSupport {

	/**
	 * An 8 x 8 x 8 test area of air, from this test mod's data (the same size
	 * as Fabric's own empty structure), so every loader runs the tests in the
	 * same space.
	 */
	static final String EMPTY_STRUCTURE = "secretblocks_gametest:empty";

	private static final GameProfile PROFILE = new GameProfile(
			UUID.nameUUIDFromBytes("secretblocks-gametest".getBytes(StandardCharsets.UTF_8)), "sb_gametest");

	private TestSupport() {
	}

	/** A stone floor under the whole 8x8 test area, at relative y = 0. */
	static void floor(GameTestHelper h) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(x, 0, z, Blocks.STONE);
			}
		}
	}

	static Vec3 abs(GameTestHelper h, Vec3 rel) {
		BlockPos origin = h.absolutePos(BlockPos.ZERO);
		return rel.add(origin.getX(), origin.getY(), origin.getZ());
	}

	/** A server player standing at {@code feetRel} whose eyes look exactly at {@code lookAtRel}. */
	static ServerPlayer player(GameTestHelper h, Vec3 feetRel, Vec3 lookAtRel) {
		ServerLevel level = h.getLevel();
		ServerPlayer player = new ServerPlayer(level.getServer(), level, PROFILE);
		Vec3 feet = abs(h, feetRel);
		Vec3 eye = feet.add(0, player.getEyeHeight(), 0);
		Vec3 d = abs(h, lookAtRel).subtract(eye);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		player.moveTo(feet.x, feet.y, feet.z, yaw, pitch);
		// A living entity looks where its head points (getViewVector uses the
		// head rotation), and moveTo only sets the body rotation.
		player.setYHeadRot(yaw);
		player.yHeadRotO = yaw;
		return player;
	}

	/**
	 * Use {@code stack} on the face {@code face} of the block at {@code clickedRel},
	 * hitting the point {@code hitRel}, as a player standing at {@code feetRel}
	 * and looking at that point.
	 */
	static InteractionResult useOn(GameTestHelper h, ServerPlayer player, ItemStack stack,
			BlockPos clickedRel, Direction face, Vec3 hitRel) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hit = new BlockHitResult(abs(h, hitRel), face, h.absolutePos(clickedRel), false);
		return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	/** Use {@code stack} with no player at all, as automation (dispensers of other mods, deployers) may. */
	static InteractionResult useOnWithoutPlayer(GameTestHelper h, ItemStack stack, BlockPos clickedRel, Direction face, Vec3 hitRel) {
		BlockHitResult hit = new BlockHitResult(abs(h, hitRel), face, h.absolutePos(clickedRel), false);
		// UseOnContext's full constructor is protected in vanilla; a subclass may call it.
		return stack.useOn(new UseOnContext(h.getLevel(), null, InteractionHand.MAIN_HAND, stack, hit) {
		});
	}

	static InteractionResult place(GameTestHelper h, Item item, Vec3 feetRel,
			BlockPos clickedRel, Direction face, Vec3 hitRel) {
		return useOn(h, player(h, feetRel, hitRel), new ItemStack(item), clickedRel, face, hitRel);
	}

	static DisguisedBlockEntity secret(GameTestHelper h, BlockPos rel) {
		BlockEntity entity = h.getBlockEntity(rel);
		if (entity instanceof DisguisedBlockEntity secret) {
			return secret;
		}
		h.fail("expected a secret block entity, found " + entity, rel);
		throw new AssertionError("unreachable");
	}

	/** The block entity at {@code rel} as a Secret Chest's container block entity. */
	static SecretChestBlockEntity chest(GameTestHelper h, BlockPos rel) {
		BlockEntity entity = h.getBlockEntity(rel);
		if (entity instanceof SecretChestBlockEntity chest) {
			return chest;
		}
		h.fail("expected a SecretChestBlockEntity, found " + entity, rel);
		throw new AssertionError("unreachable");
	}

	/** The block entity at {@code rel} as the plain {@link SecretBlockEntity} (disguise, full NBT). */
	static SecretBlockEntity plain(GameTestHelper h, BlockPos rel) {
		BlockEntity entity = h.getBlockEntity(rel);
		if (entity instanceof SecretBlockEntity secret) {
			return secret;
		}
		h.fail("expected a SecretBlockEntity, found " + entity, rel);
		throw new AssertionError("unreachable");
	}

	/** What the crafting table makes from these stacks laid out from the top-left slot, if any recipe matches. */
	static Optional<ItemStack> craft(GameTestHelper h, ItemStack... stacks) {
		AbstractContainerMenu menu = new AbstractContainerMenu(null, 0) {
			@Override
			public ItemStack quickMoveStack(Player player, int index) {
				return ItemStack.EMPTY;
			}

			@Override
			public boolean stillValid(Player player) {
				return true;
			}
		};
		CraftingContainer grid = new net.minecraft.world.inventory.TransientCraftingContainer(menu, 3, 3);
		for (int i = 0; i < stacks.length; i++) {
			grid.setItem(i, stacks[i]);
		}
		return h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, h.getLevel())
				.map((CraftingRecipe recipe) -> recipe.assemble(grid, h.getLevel().registryAccess()));
	}

	/** {@link #craft(GameTestHelper, ItemStack...)} with one of each item. */
	static Optional<ItemStack> craft(GameTestHelper h, Item... items) {
		ItemStack[] stacks = new ItemStack[items.length];
		for (int i = 0; i < items.length; i++) {
			stacks[i] = new ItemStack(items[i]);
		}
		return craft(h, stacks);
	}

	static void check(GameTestHelper h, boolean condition, String message) {
		if (!condition) {
			h.fail(message);
		}
	}

	static void checkBlock(GameTestHelper h, BlockPos rel, Block expected) {
		BlockState actual = h.getBlockState(rel);
		if (!actual.is(expected)) {
			h.fail("expected " + expected + " but found " + actual, rel);
		}
	}

	/** Every face of the secret block at {@code rel} is disguised as {@code all}, except the listed overrides. */
	static void checkFaces(GameTestHelper h, BlockPos rel, Block all, Map<Direction, Block> overrides) {
		DisguisedBlockEntity secret = secret(h, rel);
		for (Direction dir : Direction.values()) {
			Block expected = overrides.getOrDefault(dir, all);
			BlockState actual = secret.getState(dir);
			if (!actual.is(expected)) {
				h.fail(dir + " face: expected " + expected + " but found " + actual, rel);
			}
		}
	}

	static void checkFaces(GameTestHelper h, BlockPos rel, Block all) {
		checkFaces(h, rel, all, Map.of());
	}

	static Map<Direction, Block> only(Direction dir, Block block) {
		Map<Direction, Block> map = new EnumMap<>(Direction.class);
		map.put(dir, block);
		return map;
	}
}
