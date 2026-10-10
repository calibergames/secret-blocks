package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.blocks.entity.SecretDispenserBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.mojang.authlib.GameProfile;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Dropper is a vanilla dropper that keeps a disguise. Most tests
 * run a vanilla dropper beside it and compare. Layout: stone floor at y = 0;
 * the Secret Dropper at x = 1, the vanilla one at x = 5, both at z = 3
 * facing north unless a test says otherwise.
 */
public class DropperGameTests implements FabricGameTest {

	private static final Block SECRET = ModBlocks.SECRET_DROPPER;
	private static final BlockPos SECRET_POS = new BlockPos(1, 1, 3);
	private static final BlockPos VANILLA_POS = new BlockPos(5, 1, 3);
	private static final BlockPos[] BOTH = {SECRET_POS, VANILLA_POS};

	private static BlockState facing(Block block, Direction direction) {
		return block.defaultBlockState().setValue(DispenserBlock.FACING, direction);
	}

	/** Places the Secret Dropper and the vanilla one, both facing {@code direction}. */
	private static void placeBoth(GameTestHelper h, Direction direction) {
		h.setBlock(SECRET_POS, facing(SECRET, direction));
		h.setBlock(VANILLA_POS, facing(Blocks.DROPPER, direction));
	}

	private static DispenserBlockEntity dropper(GameTestHelper h, BlockPos rel) {
		BlockEntity entity = h.getBlockEntity(rel);
		if (entity instanceof DispenserBlockEntity dropper) {
			return dropper;
		}
		h.fail("expected a dropper block entity, found " + entity, rel);
		throw new AssertionError("unreachable");
	}

	private static SecretDispenserBlockEntity secretDropper(GameTestHelper h) {
		if (h.getBlockEntity(SECRET_POS) instanceof SecretDispenserBlockEntity dropper) {
			return dropper;
		}
		h.fail("expected a SecretDispenserBlockEntity, found " + h.getBlockEntity(SECRET_POS), SECRET_POS);
		throw new AssertionError("unreachable");
	}

	private static String name(BlockPos rel) {
		return rel.equals(SECRET_POS) ? "the Secret Dropper" : "the vanilla dropper";
	}

	/** A named stack, so that items thrown by other tests are never counted. */
	private static ItemStack probe(Item item, int count, String test) {
		return new ItemStack(item, count).setHoverName(Component.literal("Test dropper " + test));
	}

	private static int count(Container container) {
		int total = 0;
		for (int i = 0; i < container.getContainerSize(); i++) {
			total += container.getItem(i).getCount();
		}
		return total;
	}

	/** Thrown items with the test's name within {@code reach} of a block. */
	private static List<ItemEntity> thrown(GameTestHelper h, BlockPos rel, double reach, String test) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(reach),
				e -> e.getItem().getHoverName().getString().equals("Test dropper " + test));
	}

	private static int thrownCount(GameTestHelper h, BlockPos rel, double reach, String test) {
		return thrown(h, rel, reach, test).stream().mapToInt(e -> e.getItem().getCount()).sum();
	}

	private static Map<Direction, BlockState> all(Block block) {
		Map<Direction, BlockState> faces = new EnumMap<>(Direction.class);
		for (Direction dir : Direction.values()) {
			faces.put(dir, block.defaultBlockState());
		}
		return faces;
	}

	/** Switches both droppers on (a redstone block behind each) or off. */
	private static void power(GameTestHelper h, boolean on) {
		for (BlockPos pos : BOTH) {
			h.setBlock(pos.south(), on ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
		}
	}

	/** A glass cell around the block in front of each dropper, so thrown items stay put. */
	private static void cells(GameTestHelper h) {
		for (BlockPos pos : BOTH) {
			BlockPos cell = pos.north();
			for (Direction side : new Direction[] {Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP}) {
				h.setBlock(cell.relative(side), Blocks.GLASS);
			}
		}
	}

	/** Placed through the item, it faces the way a vanilla dropper would, and copies the clicked block. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void facingMatchesVanilla(GameTestHelper h) {
		floor(h);
		place(h, SECRET.asItem(), new Vec3(1.5, 1, 6.5), new BlockPos(1, 0, 3), Direction.UP, new Vec3(1.5, 1.0, 3.5));
		place(h, Items.DROPPER, new Vec3(5.5, 1, 6.5), new BlockPos(5, 0, 3), Direction.UP, new Vec3(5.5, 1.0, 3.5));
		place(h, SECRET.asItem(), new Vec3(1.5, 1, 1.9), new BlockPos(1, 0, 1), Direction.UP, new Vec3(1.5, 1.0, 1.5));
		place(h, Items.DROPPER, new Vec3(5.5, 1, 1.9), new BlockPos(5, 0, 1), Direction.UP, new Vec3(5.5, 1.0, 1.5));
		for (int z : new int[] {3, 1}) {
			checkBlock(h, new BlockPos(1, 1, z), SECRET);
			checkBlock(h, new BlockPos(5, 1, z), Blocks.DROPPER);
			Direction secret = h.getBlockState(new BlockPos(1, 1, z)).getValue(DispenserBlock.FACING);
			Direction vanilla = h.getBlockState(new BlockPos(5, 1, z)).getValue(DispenserBlock.FACING);
			check(h, secret == vanilla, "the Secret Dropper faces " + secret + ", the vanilla one " + vanilla);
		}
		checkFaces(h, new BlockPos(1, 1, 3), Blocks.STONE);
		h.succeed();
	}

	/**
	 * A redstone signal drops one item out of the facing side; holding the
	 * signal does not repeat it. An arrow is dropped as an item, not shot (a
	 * dispenser shoots it).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void dropsOneItemPerRisingEdge(GameTestHelper h) {
		floor(h);
		cells(h);
		placeBoth(h, Direction.NORTH);
		for (BlockPos pos : BOTH) {
			dropper(h, pos).setItem(0, probe(Items.ARROW, 5, "edge"));
		}
		power(h, true);
		h.runAfterDelay(30, () -> {
			for (BlockPos pos : BOTH) {
				int out = thrownCount(h, pos.north(), 0.4, "edge");
				check(h, out == 1, name(pos) + " should have dropped one arrow into the block in front, " + out + " found there");
				check(h, count(dropper(h, pos)) == 4, name(pos) + " should hold 4 arrows while the signal is held, holds " + count(dropper(h, pos)));
				check(h, h.getLevel().getEntitiesOfClass(Arrow.class, new AABB(h.absolutePos(pos)).inflate(3)).isEmpty(), name(pos) + " must not shoot the arrow");
				check(h, h.getBlockState(pos).getValue(DispenserBlock.TRIGGERED), name(pos) + " should be triggered");
			}
			thrown(h, new BlockPos(3, 1, 2), 4, "edge").forEach(ItemEntity::discard);
			h.succeed();
		});
	}

	/** Facing a chest, it puts the item inside and throws nothing. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void pushesIntoTheContainerItFaces(GameTestHelper h) {
		floor(h);
		placeBoth(h, Direction.NORTH);
		for (BlockPos pos : BOTH) {
			h.setBlock(pos.north(), Blocks.CHEST);
			dropper(h, pos).setItem(4, probe(Items.DIAMOND, 3, "push"));
		}
		power(h, true);
		h.runAfterDelay(20, () -> {
			for (BlockPos pos : BOTH) {
				int inChest = count((Container) h.getBlockEntity(pos.north()));
				check(h, inChest == 1, name(pos) + " should have put one diamond in the chest, the chest holds " + inChest);
				check(h, count(dropper(h, pos)) == 2, name(pos) + " should hold 2 diamonds, holds " + count(dropper(h, pos)));
			}
			check(h, thrown(h, new BlockPos(3, 1, 2), 5, "push").isEmpty(), "no diamond should have been thrown out");
			h.succeed();
		});
	}

	/** Facing a full chest, it keeps the item: nothing is thrown and nothing lost. With room made, the next pulse moves one. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void keepsTheItemWhenTheContainerIsFull(GameTestHelper h) {
		floor(h);
		placeBoth(h, Direction.NORTH);
		for (BlockPos pos : BOTH) {
			h.setBlock(pos.north(), Blocks.CHEST);
			Container chest = (Container) h.getBlockEntity(pos.north());
			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
			}
			dropper(h, pos).setItem(0, probe(Items.DIAMOND, 3, "full"));
		}
		power(h, true);
		h.runAfterDelay(20, () -> {
			for (BlockPos pos : BOTH) {
				check(h, h.getBlockState(pos).getValue(DispenserBlock.TRIGGERED), name(pos) + " should have been triggered");
				check(h, count(dropper(h, pos)) == 3, name(pos) + " should still hold its 3 diamonds, holds " + count(dropper(h, pos)));
				// Room for one stack now; the next pulse must move a diamond (so the first pulse did run, and kept it).
				((Container) h.getBlockEntity(pos.north())).setItem(0, ItemStack.EMPTY);
			}
			check(h, thrown(h, new BlockPos(3, 1, 2), 5, "full").isEmpty(), "no diamond should have been thrown out");
			power(h, false);
		});
		h.runAfterDelay(25, () -> power(h, true));
		h.runAfterDelay(40, () -> {
			for (BlockPos pos : BOTH) {
				ItemStack moved = ((Container) h.getBlockEntity(pos.north())).getItem(0);
				check(h, moved.is(Items.DIAMOND) && moved.getCount() == 1, name(pos) + " should put one diamond in the freed slot, the slot holds " + moved);
				check(h, count(dropper(h, pos)) == 2, name(pos) + " should hold 2 diamonds after the second pulse, holds " + count(dropper(h, pos)));
			}
			check(h, thrown(h, new BlockPos(3, 1, 2), 5, "full").isEmpty(), "no diamond should have been thrown out");
			h.succeed();
		});
	}

	/**
	 * Secret containers and droppers work together: a Secret Dropper fills a
	 * Secret Chest and a Secret Hopper, and a vanilla dropper fills a Secret
	 * Dropper.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void worksWithTheOtherSecretContainers(GameTestHelper h) {
		floor(h);
		Block[][] rows = {
				{SECRET, ModBlocks.SECRET_CHEST},
				{SECRET, ModBlocks.SECRET_HOPPER},
				{Blocks.DROPPER, SECRET},
				{SECRET, SECRET}};
		for (int i = 0; i < rows.length; i++) {
			BlockPos from = new BlockPos(i * 2, 1, 3);
			h.setBlock(from.north(), rows[i][1]);
			h.setBlock(from, facing(rows[i][0], Direction.NORTH));
			dropper(h, from).setItem(0, probe(Items.DIAMOND, 2, "secret"));
			h.setBlock(from.south(), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(20, () -> {
			for (int i = 0; i < rows.length; i++) {
				BlockPos from = new BlockPos(i * 2, 1, 3);
				Container target = HopperBlockEntity.getContainerAt(h.getLevel(), h.absolutePos(from.north()));
				check(h, target != null && count(target) == 1, rows[i][0] + " should have put one diamond in " + rows[i][1] + ", it holds " + (target == null ? "nothing: not a container" : count(target)));
				check(h, count(dropper(h, from)) == 1, rows[i][0] + " facing " + rows[i][1] + " should hold 1 diamond, holds " + count(dropper(h, from)));
			}
			check(h, thrown(h, new BlockPos(3, 1, 2), 5, "secret").isEmpty(), "no diamond should have been thrown out");
			h.succeed();
		});
	}

	/**
	 * Item conservation into a chest: 64 pulses move 64 items, one per pulse,
	 * and at every pulse the dropper and the chest together hold exactly 64.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 450)
	public void sixtyFourPulsesMoveSixtyFourItemsIntoAChest(GameTestHelper h) {
		floor(h);
		placeBoth(h, Direction.NORTH);
		for (BlockPos pos : BOTH) {
			h.setBlock(pos.north(), Blocks.CHEST);
			// Spread over several slots: the dropper picks a random filled slot each time.
			dropper(h, pos).setItem(0, probe(Items.DIAMOND, 30, "chest64"));
			dropper(h, pos).setItem(4, probe(Items.DIAMOND, 30, "chest64"));
			dropper(h, pos).setItem(8, probe(Items.DIAMOND, 4, "chest64"));
		}
		for (int pulse = 0; pulse < 64; pulse++) {
			int moved = pulse + 1;
			h.runAfterDelay(pulse * 6L, () -> power(h, true));
			h.runAfterDelay(pulse * 6L + 3, () -> power(h, false));
			h.runAfterDelay(pulse * 6L + 5, () -> {
				for (BlockPos pos : BOTH) {
					int inChest = count((Container) h.getBlockEntity(pos.north()));
					int left = count(dropper(h, pos));
					check(h, inChest == moved && left == 64 - moved,
							"after pulse " + moved + ", " + name(pos) + " should hold " + (64 - moved) + " and its chest " + moved + "; they hold " + left + " and " + inChest);
				}
			});
		}
		h.runAfterDelay(64 * 6L + 6, () -> {
			check(h, thrown(h, new BlockPos(3, 1, 2), 5, "chest64").isEmpty(), "no diamond should have been thrown out");
			h.succeed();
		});
	}

	/**
	 * Item conservation into the open: 64 pulses throw 64 items, and at every
	 * pulse the dropper and the thrown items together make exactly 64. A glass
	 * cell in front of each dropper keeps the items in place.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 450)
	public void sixtyFourPulsesThrowSixtyFourItems(GameTestHelper h) {
		floor(h);
		cells(h);
		placeBoth(h, Direction.NORTH);
		for (BlockPos pos : BOTH) {
			dropper(h, pos).setItem(0, probe(Items.DIAMOND, 30, "open64"));
			dropper(h, pos).setItem(4, probe(Items.DIAMOND, 30, "open64"));
			dropper(h, pos).setItem(8, probe(Items.DIAMOND, 4, "open64"));
		}
		for (int pulse = 0; pulse < 64; pulse++) {
			int moved = pulse + 1;
			h.runAfterDelay(pulse * 6L, () -> power(h, true));
			h.runAfterDelay(pulse * 6L + 3, () -> power(h, false));
			h.runAfterDelay(pulse * 6L + 5, () -> {
				for (BlockPos pos : BOTH) {
					int out = thrownCount(h, pos.north(), 0.4, "open64");
					int left = count(dropper(h, pos));
					check(h, out == moved && left == 64 - moved,
							"after pulse " + moved + ", " + name(pos) + " should hold " + (64 - moved) + " with " + moved + " thrown; it holds " + left + " with " + out + " thrown");
				}
			});
		}
		h.runAfterDelay(64 * 6L + 6, () -> {
			thrown(h, new BlockPos(3, 1, 2), 5, "open64").forEach(ItemEntity::discard);
			h.succeed();
		});
	}

	/** Dropping toggles the TRIGGERED state; the disguise must survive it. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void keepsDisguiseWhenTriggered(GameTestHelper h) {
		floor(h);
		h.setBlock(SECRET_POS, facing(SECRET, Direction.NORTH));
		h.setBlock(SECRET_POS.north(), Blocks.CHEST);
		secretDropper(h).setDisguise(all(Blocks.BRICKS));
		secretDropper(h).setItem(0, probe(Items.DIAMOND, 1, "disguise"));
		h.setBlock(SECRET_POS.south(), Blocks.REDSTONE_BLOCK);
		h.runAfterDelay(20, () -> {
			check(h, h.getBlockState(SECRET_POS).getValue(DispenserBlock.TRIGGERED), "the dropper should be triggered");
			checkFaces(h, SECRET_POS, Blocks.BRICKS);
			h.setBlock(SECRET_POS.south(), Blocks.AIR);
		});
		h.runAfterDelay(30, () -> {
			check(h, !h.getBlockState(SECRET_POS).getValue(DispenserBlock.TRIGGERED), "the dropper should be untriggered");
			checkFaces(h, SECRET_POS, Blocks.BRICKS);
			h.succeed();
		});
	}

	/** It opens the vanilla 3×3 screen, titled with its own name. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void opensTheVanillaScreenTitledSecretDropper(GameTestHelper h) {
		placeBoth(h, Direction.NORTH);
		Player player = h.makeMockPlayer();
		AbstractContainerMenu menu = secretDropper(h).createMenu(0, player.getInventory(), player);
		AbstractContainerMenu vanilla = dropper(h, VANILLA_POS).createMenu(0, player.getInventory(), player);
		check(h, vanilla instanceof DispenserMenu, "control: a vanilla dropper opens a DispenserMenu, got " + vanilla);
		check(h, menu instanceof DispenserMenu, "expected a vanilla DispenserMenu, got " + menu);
		check(h, secretDropper(h).getDisplayName().getContents() instanceof TranslatableContents title
				&& title.getKey().equals("block.secretblocks.secret_dropper"), "the title should be Secret Dropper, got " + secretDropper(h).getDisplayName());
		h.succeed();
	}

	/**
	 * Opening it counts as searching a dropper, not a dispenser, as opening a
	 * vanilla dropper does (vanilla decides by the block entity's class, which
	 * ours is not).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void openingItCountsAsADropperSearched(GameTestHelper h) {
		floor(h);
		placeBoth(h, Direction.NORTH);
		for (BlockPos pos : BOTH) {
			List<ResourceLocation> stats = new ArrayList<>();
			List<MenuProvider> opened = new ArrayList<>();
			Player player = new Player(h.getLevel(), h.absolutePos(pos.north()), 0.0F, new GameProfile(UUID.randomUUID(), "test_dropper")) {
				@Override
				public boolean isSpectator() {
					return false;
				}

				@Override
				public boolean isCreative() {
					return false;
				}

				@Override
				public void awardStat(ResourceLocation stat) {
					stats.add(stat);
				}

				@Override
				public OptionalInt openMenu(MenuProvider menu) {
					opened.add(menu);
					return OptionalInt.empty();
				}
			};
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.NORTH, h.absolutePos(pos), false);
			InteractionResult result = h.getBlockState(pos).use(h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
			check(h, result == InteractionResult.CONSUME, "using " + name(pos) + " should be consumed on the server, got " + result);
			check(h, opened.size() == 1 && opened.get(0) == h.getBlockEntity(pos), name(pos) + " should open its own block entity's screen, opened " + opened);
			check(h, stats.equals(List.of(Stats.INSPECT_DROPPER)), name(pos) + " should count as one dropper searched, counted " + stats);
		}
		h.succeed();
	}

	/** Contents and disguise survive a save and load of the block entity, as a Secret Dropper. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void contentsAndDisguiseSurviveSaveAndLoad(GameTestHelper h) {
		h.setBlock(SECRET_POS, facing(SECRET, Direction.NORTH));
		secretDropper(h).setDisguise(all(Blocks.BRICKS));
		secretDropper(h).setItem(0, new ItemStack(Items.DIAMOND, 5));
		secretDropper(h).setItem(8, new ItemStack(Items.ARROW, 64));
		secretDropper(h).setDirection(Direction.NORTH, Direction.UP);
		secretDropper(h).setRotation(Direction.EAST, 180);
		CompoundTag saved = secretDropper(h).saveWithFullMetadata();
		check(h, saved.getString("id").equals("secretblocks:secret_dispenser"), "it is saved as the shared type secretblocks:secret_dispenser, got " + saved.getString("id"));
		// The shared type must list the dropper: the game checks it when it looks a block entity up by type.
		check(h, secretDropper(h).getType().isValid(h.getBlockState(SECRET_POS)), "the block entity type " + saved.getString("id") + " should accept the Secret Dropper");
		check(h, h.getLevel().getBlockEntity(h.absolutePos(SECRET_POS), secretDropper(h).getType()).isPresent(), "the Secret Dropper's block entity should be found by its type");
		BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(SECRET_POS), h.getBlockState(SECRET_POS), saved);
		check(h, loaded instanceof SecretDispenserBlockEntity, "loads back as a SecretDispenserBlockEntity, got " + loaded);
		SecretDispenserBlockEntity copy = (SecretDispenserBlockEntity) loaded;
		check(h, ItemStack.matches(copy.getItem(0), new ItemStack(Items.DIAMOND, 5)), "slot 0 should hold 5 diamonds");
		check(h, ItemStack.matches(copy.getItem(8), new ItemStack(Items.ARROW, 64)), "slot 8 should hold 64 arrows");
		for (Direction dir : Direction.values()) {
			check(h, copy.getState(dir).is(Blocks.BRICKS), dir + " face should still be bricks");
		}
		check(h, copy.getDir(Direction.NORTH) == Direction.UP, "north face direction should survive");
		check(h, copy.getRotation(Direction.EAST) == 180, "east face rotation should survive");
		check(h, copy.getDisplayName().getContents() instanceof TranslatableContents title && title.getKey().equals("block.secretblocks.secret_dropper"),
				"loaded, it should still be titled Secret Dropper, got " + copy.getDisplayName());
		h.succeed();
	}

	/** Clients receive the disguise, never the contents. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void clientsGetTheDisguiseNotTheContents(GameTestHelper h) {
		h.setBlock(SECRET_POS, facing(SECRET, Direction.NORTH));
		secretDropper(h).setDisguise(all(Blocks.BRICKS));
		secretDropper(h).setItem(0, new ItemStack(Items.DIAMOND, 5));
		CompoundTag update = secretDropper(h).getUpdateTag();
		check(h, update.contains("northState") && !update.contains("Items"), "the update tag should carry the disguise only: " + update);
		CompoundTag packet = secretDropper(h).getUpdatePacket().getTag();
		check(h, packet != null && packet.getCompound("northState").getString("Name").equals("minecraft:bricks") && !packet.contains("Items"),
				"the update packet should carry the disguise only: " + packet);
		h.succeed();
	}

	/** Broken, it drops its contents and itself, keeping a custom name, as a vanilla dropper does. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void dropsContentsAndItselfWhenBroken(GameTestHelper h) {
		floor(h);
		placeBoth(h, Direction.NORTH);
		Item[] items = {SECRET.asItem(), Items.DROPPER};
		for (BlockPos pos : BOTH) {
			dropper(h, pos).setItem(4, probe(Items.DIAMOND, 3, "broken"));
			dropper(h, pos).setCustomName(Component.literal("Trap"));
			h.getLevel().destroyBlock(h.absolutePos(pos), true);
		}
		for (int i = 0; i < BOTH.length; i++) {
			BlockPos pos = BOTH[i];
			Item item = items[i];
			checkBlock(h, pos, Blocks.AIR);
			int diamonds = thrownCount(h, pos, 1.5, "broken");
			check(h, diamonds == 3, name(pos) + " should drop its 3 diamonds, dropped " + diamonds);
			List<ItemEntity> self = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(pos)).inflate(1.5), e -> e.getItem().is(item));
			check(h, self.size() == 1 && self.get(0).getItem().getCount() == 1 && self.get(0).getItem().getHoverName().getString().equals("Trap"),
					name(pos) + " should drop itself once, named Trap; dropped " + self.stream().map(e -> e.getItem().toString() + " " + e.getItem().getHoverName().getString()).toList());
			self.forEach(ItemEntity::discard);
			thrown(h, pos, 1.5, "broken").forEach(ItemEntity::discard);
		}
		h.succeed();
	}

	/** A comparator reads its contents as it reads a vanilla dropper's. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void comparatorReadsIt(GameTestHelper h) {
		floor(h);
		for (BlockPos pos : BOTH) {
			h.setBlock(pos.north().north(), Blocks.REDSTONE_LAMP);
			h.setBlock(pos.north(), Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.SOUTH));
		}
		placeBoth(h, Direction.UP);
		for (BlockPos pos : BOTH) {
			dropper(h, pos).setItem(0, new ItemStack(Items.DIAMOND, 64));
			dropper(h, pos).setItem(1, new ItemStack(Items.DIAMOND, 64));
			dropper(h, pos).setChanged();
		}
		h.runAfterDelay(10, () -> {
			int secret = h.getBlockState(SECRET_POS).getAnalogOutputSignal(h.getLevel(), h.absolutePos(SECRET_POS));
			int vanilla = h.getBlockState(VANILLA_POS).getAnalogOutputSignal(h.getLevel(), h.absolutePos(VANILLA_POS));
			check(h, vanilla == 4, "control: a vanilla dropper with 2 of 9 slots full reads 4, reads " + vanilla);
			check(h, secret == vanilla, "the Secret Dropper should read " + vanilla + " on a comparator, reads " + secret);
			for (BlockPos pos : BOTH) {
				check(h, h.getBlockState(pos.north().north()).getValue(RedstoneLampBlock.LIT), "the lamp behind the comparator at " + name(pos) + " should light");
			}
			h.succeed();
		});
	}

	/**
	 * What it is made of: wool-like toughness and, like a vanilla dropper,
	 * a conductor that pistons cannot move and that gives no light or signal.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void properties(GameTestHelper h) {
		floor(h);
		placeBoth(h, Direction.NORTH);
		BlockState secret = h.getBlockState(SECRET_POS);
		BlockState vanilla = h.getBlockState(VANILLA_POS);
		BlockPos abs = h.absolutePos(SECRET_POS);
		check(h, secret.getDestroySpeed(h.getLevel(), abs) == Blocks.WHITE_WOOL.defaultBlockState().getDestroySpeed(h.getLevel(), abs),
				"it should be as hard as wool (0.8), is " + secret.getDestroySpeed(h.getLevel(), abs));
		check(h, !secret.requiresCorrectToolForDrops(), "it should drop without a tool");
		check(h, vanilla.isRedstoneConductor(h.getLevel(), h.absolutePos(VANILLA_POS)) && secret.isRedstoneConductor(h.getLevel(), abs), "it should be a conductor, as a vanilla dropper is");
		check(h, secret.getLightEmission() == 0 && !secret.isSignalSource() && !vanilla.isSignalSource(), "it should give no light and no signal");
		check(h, secret.hasAnalogOutputSignal() && vanilla.hasAnalogOutputSignal(), "comparators should read it");
		check(h, secret.hasBlockEntity() && secret.getPistonPushReaction() != PushReaction.DESTROY, "a piston must not break it");
		check(h, secret.isCollisionShapeFullBlock(h.getLevel(), abs), "it should be a full block");
		h.succeed();
	}

	/** Dummy Block + dropper makes one; a dispenser does not. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void recipeWorks(GameTestHelper h) {
		Optional<ItemStack> result = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.DROPPER);
		check(h, result.isPresent() && result.get().is(SECRET.asItem()) && result.get().getCount() == 1,
				"Dummy Block + dropper should craft one Secret Dropper, got " + result);
		Optional<ItemStack> dispenser = craft(h, ModBlocks.SOLID_BLOCK.asItem(), Items.DISPENSER);
		check(h, dispenser.isPresent() && dispenser.get().is(ModBlocks.SECRET_DISPENSER.asItem()), "Dummy Block + dispenser should still craft a Secret Dispenser, got " + dispenser);
		h.succeed();
	}
}
