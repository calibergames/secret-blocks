package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.blocks.entity.SecretBlockEntity;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.Set;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * Secret blocks keep vanilla behavior. The Secret Chest, Dispenser and
 * Hopper are containers exactly like their vanilla twins; every other secret
 * block is not a container, like the vanilla block it stands in for (before
 * 2.0.0 hoppers pushed items into them and the items were lost).
 */
public class ContainerGameTests implements FabricGameTest {

	/** The secret blocks that are containers, as their vanilla counterparts are. */
	private static final Set<Block> CONTAINERS = Set.of(ModBlocks.SECRET_CHEST, ModBlocks.SECRET_TRAPPED_CHEST, ModBlocks.SECRET_DISPENSER, ModBlocks.SECRET_DROPPER, ModBlocks.SECRET_HOPPER);

	private static int count(Container container) {
		int total = 0;
		for (int i = 0; i < container.getContainerSize(); i++) {
			total += container.getItem(i).getCount();
		}
		return total;
	}

	private static Container container(GameTestHelper h, BlockPos rel) {
		return HopperBlockEntity.getContainerAt(h.getLevel(), h.absolutePos(rel));
	}

	private static void hopperAbove(GameTestHelper h, BlockPos target, int diamonds) {
		BlockPos hopper = target.above();
		h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
		((HopperBlockEntity) h.getBlockEntity(hopper)).setItem(0, new ItemStack(Items.DIAMOND, diamonds));
	}

	/** Every secret block with a block entity: a container only if its vanilla counterpart is one. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyTheSecretContainersAreContainers(GameTestHelper h) {
		floor(h);
		BlockPos rel = new BlockPos(1, 1, 1);
		int checked = 0;
		for (Block block : Registry.BLOCK) {
			if (!Registry.BLOCK.getKey(block).getNamespace().equals(SecretBlocks.MOD_ID) || !(block instanceof EntityBlock)) {
				continue;
			}
			h.setBlock(rel, block.defaultBlockState());
			boolean isContainer = container(h, rel) != null;
			check(h, isContainer == CONTAINERS.contains(block), Registry.BLOCK.getKey(block)
					+ (isContainer ? " is a container (hoppers would feed it)" : " should be a container"));
			h.setBlock(rel, Blocks.AIR);
			checked++;
		}
		check(h, checked >= 20, "expected every secret block with a block entity, checked " + checked);
		h.succeed();
	}

	/** A hopper feeds the Secret Chest, Dispenser, Hopper and Dropper exactly as it feeds the vanilla ones beside them. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void hoppersFeedTheSecretContainersLikeVanilla(GameTestHelper h) {
		floor(h);
		Block[][] pairs = {
				{ModBlocks.SECRET_CHEST, Blocks.CHEST},
				{ModBlocks.SECRET_DISPENSER, Blocks.DISPENSER},
				{ModBlocks.SECRET_HOPPER, Blocks.HOPPER},
				{ModBlocks.SECRET_DROPPER, Blocks.DROPPER},
		};
		for (int i = 0; i < pairs.length; i++) {
			for (int side = 0; side < 2; side++) {
				BlockPos target = new BlockPos(1 + i * 2, 1, 1 + side * 3);
				h.setBlock(target, pairs[i][side]);
				hopperAbove(h, target, 3);
			}
		}
		h.runAfterDelay(60, () -> {
			for (int i = 0; i < pairs.length; i++) {
				int secret = count(container(h, new BlockPos(1 + i * 2, 1, 1)));
				int vanilla = count(container(h, new BlockPos(1 + i * 2, 1, 4)));
				check(h, vanilla == 3, "control: the vanilla " + pairs[i][1] + " should receive 3, got " + vanilla);
				check(h, secret == vanilla, pairs[i][0] + " received " + secret + ", the vanilla one " + vanilla);
			}
			h.succeed();
		});
	}

	/** A hopper over a plain secret block keeps its items, as over stone (it used to push them inside, lost). */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void hoppersDoNotFeedPlainSecretBlocks(GameTestHelper h) {
		floor(h);
		Block[] targets = {Blocks.STONE, ModBlocks.SOLID_BLOCK, ModBlocks.STAIR_BLOCK, ModBlocks.SECRET_OBSERVER, ModBlocks.PRESSURE_PLATE};
		for (int i = 0; i < targets.length; i++) {
			BlockPos target = new BlockPos(1 + i, 1, 2);
			h.setBlock(target, targets[i]);
			hopperAbove(h, target, 3);
		}
		h.runAfterDelay(60, () -> {
			for (int i = 0; i < targets.length; i++) {
				BlockPos target = new BlockPos(1 + i, 1, 2);
				int kept = count((Container) h.getBlockEntity(target.above()));
				check(h, kept == 3, "the hopper over " + targets[i] + " should keep its 3 diamonds, has " + kept);
				if (h.getBlockEntity(target) instanceof SecretBlockEntity secret) {
					CompoundTag saved = secret.saveWithoutMetadata();
					check(h, saved.getList("Items", Tag.TAG_COMPOUND).isEmpty(), targets[i] + " must hold no items: " + saved.get("Items"));
				}
			}
			h.succeed();
		});
	}

	/**
	 * A dropper facing a plain secret block throws its item out as it does
	 * facing stone; facing a Secret Chest it puts the item in, as into a chest.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void droppersTreatSecretBlocksLikeVanilla(GameTestHelper h) {
		floor(h);
		Block[] targets = {Blocks.STONE, ModBlocks.SOLID_BLOCK, Blocks.CHEST, ModBlocks.SECRET_CHEST};
		for (int i = 0; i < targets.length; i++) {
			BlockPos dropper = new BlockPos(1 + i * 2, 1, 2);
			h.setBlock(dropper.south(), targets[i]);
			h.setBlock(dropper, Blocks.DROPPER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.SOUTH));
			((Container) h.getBlockEntity(dropper)).setItem(0, droppedDiamond());
			h.setBlock(dropper.north(), Blocks.REDSTONE_BLOCK);
		}
		h.runAfterDelay(20, () -> {
			for (int i = 0; i < targets.length; i++) {
				BlockPos dropper = new BlockPos(1 + i * 2, 1, 2);
				int left = count((Container) h.getBlockEntity(dropper));
				check(h, left == 0, "the dropper facing " + targets[i] + " should have used its diamond, has " + left);
			}
			// Facing stone or a plain secret block, the diamond is thrown out into the
			// world: the Dummy Block keeps nothing (the 1.2.0 bug put it inside), and
			// both diamonds exist as items. A diamond thrown into a solid block is
			// pushed out and can fly several blocks, so the count covers a wide area.
			CompoundTag dummy = plain(h, new BlockPos(3, 1, 3)).saveWithoutMetadata();
			check(h, dummy.getList("Items", Tag.TAG_COMPOUND).isEmpty(), "the Dummy Block must not take the dropper's diamond: " + dummy.get("Items"));
			int thrown = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(2, 1, 3))).inflate(24),
					e -> ItemStack.isSameItemSameTags(e.getItem(), droppedDiamond())).stream().mapToInt(e -> e.getItem().getCount()).sum();
			check(h, thrown == 2, "the droppers facing stone and the Dummy Block should each throw a diamond out, " + thrown + " found");
			check(h, count(container(h, new BlockPos(5, 1, 3))) == 1, "control: the vanilla chest should receive the diamond");
			check(h, count(container(h, new BlockPos(7, 1, 3))) == 1, "the Secret Chest should receive the diamond");
			h.succeed();
		});
	}

	/** A hopper under a Secret Chest pulls its items out, as under a vanilla chest. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void hoppersPullFromTheSecretChestLikeVanilla(GameTestHelper h) {
		floor(h);
		Block[] chests = {Blocks.CHEST, ModBlocks.SECRET_CHEST};
		for (int i = 0; i < chests.length; i++) {
			BlockPos hopper = new BlockPos(1 + i * 3, 1, 2);
			h.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
			h.setBlock(hopper.above(), chests[i]);
			container(h, hopper.above()).setItem(0, new ItemStack(Items.DIAMOND, 3));
		}
		h.runAfterDelay(60, () -> {
			for (int i = 0; i < chests.length; i++) {
				BlockPos hopper = new BlockPos(1 + i * 3, 1, 2);
				int pulled = count(container(h, hopper));
				check(h, pulled == 3, "the hopper under the " + chests[i] + " should have pulled 3 diamonds, has " + pulled);
			}
			h.succeed();
		});
	}

	/**
	 * The droppers' diamond, named so that the wide search for thrown items
	 * counts only these (neighbouring tests drop diamonds too).
	 */
	private static ItemStack droppedDiamond() {
		return new ItemStack(Items.DIAMOND).setHoverName(Component.literal("Dropper probe"));
	}

	private static int diamondsAround(GameTestHelper h, BlockPos rel, Item item) {
		AABB around = new AABB(h.absolutePos(rel)).inflate(1.5);
		return h.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(item))
				.stream().mapToInt(e -> e.getItem().getCount()).sum();
	}

	/**
	 * Items a hopper put into a plain secret block before 2.0.0 drop when the
	 * block is broken (every plain block, including those whose vanilla class
	 * skips the base removal code). A Secret Chest drops its contents once.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void storedItemsDropWhenBroken(GameTestHelper h) throws Exception {
		floor(h);
		CompoundTag stuck = TagParser.parseTag("{Items:[{Slot:0b,id:\"minecraft:diamond\",Count:4b}]}");
		int checked = 0;
		for (Block block : ModBlocks.secretBlocksList) {
			if (!(block instanceof EntityBlock) || CONTAINERS.contains(block)) {
				continue;
			}
			BlockPos rel = new BlockPos(3, 1, 3);
			h.setBlock(rel, block.defaultBlockState());
			// Only blocks with the shared SecretBlockEntity had the old hidden inventory. Blocks new
			// in 2.0.0 with a block entity type of their own (the Secret Comparator) never held items.
			if (!(h.getBlockEntity(rel) instanceof SecretBlockEntity)) {
				h.setBlock(rel, Blocks.AIR);
				continue;
			}
			SecretBlockEntity entity = plain(h, rel);
			entity.load(stuck.copy());
			h.setBlock(rel, Blocks.AIR);
			int dropped = diamondsAround(h, rel, Items.DIAMOND);
			check(h, dropped == 4, Registry.BLOCK.getKey(block) + " should drop the 4 stuck diamonds when broken, dropped " + dropped);
			h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(3)).forEach(ItemEntity::discard);
			checked++;
		}
		check(h, checked >= 15, "expected the plain secret blocks, checked " + checked);
		BlockPos rel = new BlockPos(3, 1, 3);
		h.setBlock(rel, ModBlocks.SECRET_CHEST);
		chest(h, rel).setItem(0, new ItemStack(Items.EMERALD, 5));
		h.setBlock(rel, Blocks.AIR);
		int emeralds = diamondsAround(h, rel, Items.EMERALD);
		check(h, emeralds == 5, "a broken Secret Chest should drop its 5 emeralds once, dropped " + emeralds);
		h.succeed();
	}

	/**
	 * In an explosion the Secret Chest and Trapped Chest drop themselves as
	 * vanilla's chests do: rarely, in a big one (the chance is 1 in the
	 * explosion's radius). Out of 50 drops at radius 100 each kind keeps
	 * fewer than five; without an explosion every one drops.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void chestsSurviveExplosionsAsVanilla(GameTestHelper h) {
		floor(h);
		BlockPos at = new BlockPos(1, 1, 1);
		for (Block block : new Block[] {ModBlocks.SECRET_CHEST, ModBlocks.SECRET_TRAPPED_CHEST, Blocks.CHEST}) {
			h.setBlock(at, block);
			int exploded = 0;
			int plain = 0;
			for (int i = 0; i < 50; i++) {
				exploded += drops(h, at, 100.0f);
				plain += drops(h, at, 0);
			}
			check(h, exploded < 5, block + " survived " + exploded + " of 50 big explosions");
			check(h, plain == 50, block + " dropped " + plain + " of 50 times without an explosion");
		}
		h.succeed();
	}

	private static int drops(GameTestHelper h, BlockPos at, float radius) {
		BlockPos abs = h.absolutePos(at);
		net.minecraft.world.level.storage.loot.LootContext.Builder loot = new net.minecraft.world.level.storage.loot.LootContext.Builder(h.getLevel())
				.withRandom(h.getLevel().getRandom())
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(abs))
				.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)
				.withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, h.getLevel().getBlockEntity(abs));
		if (radius > 0) {
			loot.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.EXPLOSION_RADIUS, radius);
		}
		return h.getBlockState(at).getDrops(loot).stream().mapToInt(ItemStack::getCount).sum();
	}

	/** The Secret Chest's screen lays out its slots exactly as vanilla's single chest does. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void chestScreenLaysOutAsVanilla(GameTestHelper h) {
		net.minecraft.world.entity.player.Player player = h.makeMockPlayer();
		net.minecraft.world.SimpleContainer box = new net.minecraft.world.SimpleContainer(27);
		net.minecraft.world.inventory.AbstractContainerMenu secret = new net.calibermc.secretblocks.screen.SecretChestScreenHandler(1, player.getInventory(), box);
		net.minecraft.world.inventory.AbstractContainerMenu vanilla = net.minecraft.world.inventory.ChestMenu.threeRows(2, player.getInventory(), box);
		check(h, secret.slots.size() == vanilla.slots.size(), "slot count " + secret.slots.size() + " vs " + vanilla.slots.size());
		for (int i = 0; i < vanilla.slots.size(); i++) {
			net.minecraft.world.inventory.Slot a = secret.slots.get(i);
			net.minecraft.world.inventory.Slot b = vanilla.slots.get(i);
			check(h, a.x == b.x && a.y == b.y && a.getContainerSlot() == b.getContainerSlot(),
					"slot " + i + " at " + a.x + "," + a.y + " (" + a.getContainerSlot() + "), vanilla at " + b.x + "," + b.y + " (" + b.getContainerSlot() + ")");
		}
		h.succeed();
	}
}
