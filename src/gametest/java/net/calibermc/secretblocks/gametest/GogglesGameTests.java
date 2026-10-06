package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.items.SecretGoggles;
import net.calibermc.secretblocks.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Secret Goggles' item behavior. The client overlay and the worn look
 * are client-side and are not covered here.
 */
public class GogglesGameTests implements FabricGameTest {

	private static ItemStack goggles() {
		return new ItemStack(ModItems.SECRET_GOGGLES);
	}

	/** A test player wearing {@code stack}, at the given wear-check tick. */
	private static Player wearer(GameTestHelper h, ItemStack stack) {
		Player player = h.makeMockPlayer();
		player.setItemSlot(EquipmentSlot.HEAD, stack);
		return player;
	}

	/** Worn: about 20% of once-a-second checks cost 1 durability (1,000 checks → about 200). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void wearsOutWhileWorn(GameTestHelper h) {
		ItemStack stack = goggles();
		Player player = wearer(h, stack);
		for (int check = 0; check < 1000; check++) {
			player.tickCount = check * SecretGoggles.WEAR_INTERVAL_TICKS;
			stack.inventoryTick(h.getLevel(), player, 39, false);
		}
		int lost = stack.getDamageValue();
		check(h, lost >= 150 && lost <= 250, "expected about 200 durability lost in 1,000 checks, lost " + lost);
		check(h, stack.getMaxDamage() == SecretGoggles.DURABILITY, "durability should be 900, is " + stack.getMaxDamage());
		h.succeed();
	}

	/** Between the once-a-second checks, and when not worn, nothing is lost. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void noWearOffTheSecondOrWhenNotWorn(GameTestHelper h) {
		ItemStack worn = goggles();
		Player player = wearer(h, worn);
		for (int tick = 1; tick < 2000; tick++) {
			if (tick % SecretGoggles.WEAR_INTERVAL_TICKS != 0) {
				player.tickCount = tick;
				worn.inventoryTick(h.getLevel(), player, 39, false);
			}
		}
		check(h, worn.getDamageValue() == 0, "no wear between checks, lost " + worn.getDamageValue());
		ItemStack carried = goggles();
		for (int check = 0; check < 500; check++) {
			player.tickCount = check * SecretGoggles.WEAR_INTERVAL_TICKS;
			carried.inventoryTick(h.getLevel(), player, 3, false);
		}
		check(h, carried.getDamageValue() == 0, "goggles carried in the inventory must not wear, lost " + carried.getDamageValue());
		h.succeed();
	}

	/** At 0 durability they break and leave the head slot. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void breakAtZero(GameTestHelper h) {
		ItemStack stack = goggles();
		stack.setDamageValue(stack.getMaxDamage() - 1);
		Player player = wearer(h, stack);
		for (int check = 0; check < 500 && !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(); check++) {
			player.tickCount = check * SecretGoggles.WEAR_INTERVAL_TICKS;
			stack.inventoryTick(h.getLevel(), player, 39, false);
		}
		check(h, player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || stack.isEmpty(), "the goggles should have broken");
		h.succeed();
	}

	/** Helmet slot, no armor value, equipped by using them. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void wornOnTheHeadWithNoArmor(GameTestHelper h) {
		Player player = h.makeMockPlayer();
		player.setItemInHand(InteractionHand.MAIN_HAND, goggles());
		player.getItemInHand(InteractionHand.MAIN_HAND).use(h.getLevel(), player, InteractionHand.MAIN_HAND);
		check(h, player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SECRET_GOGGLES), "using them should put them on the head");
		check(h, goggles().getAttributeModifiers(EquipmentSlot.HEAD).isEmpty(), "no armor or other modifiers");
		check(h, player.getArmorValue() == 0, "armor value should be 0, is " + player.getArmorValue());
		h.succeed();
	}

	/** Repair: prismarine shard is the repair item; two worn pairs combine in a crafting grid (the anvil is tested below). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void repairedWithPrismarineOrAnotherPair(GameTestHelper h) {
		check(h, ModItems.SECRET_GOGGLES.isValidRepairItem(goggles(), new ItemStack(Items.PRISMARINE_SHARD)), "prismarine shard repairs them");
		check(h, !ModItems.SECRET_GOGGLES.isValidRepairItem(goggles(), new ItemStack(Items.IRON_INGOT)), "iron does not");
		ItemStack a = goggles();
		a.setDamageValue(600);
		ItemStack b = goggles();
		b.setDamageValue(600);
		Optional<ItemStack> result = craft(h, a, b);
		check(h, result.isPresent() && result.get().is(ModItems.SECRET_GOGGLES) && result.get().getDamageValue() < 600,
				"two worn goggles should combine into a better pair, got " + result);
		h.succeed();
	}

	private static ItemStack anvil(Player player, ItemStack left, ItemStack right) {
		net.minecraft.world.inventory.AnvilMenu menu = new net.minecraft.world.inventory.AnvilMenu(0, player.getInventory(),
				net.minecraft.world.inventory.ContainerLevelAccess.NULL);
		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		menu.createResult();
		return menu.getSlot(2).getItem();
	}

	private static ItemStack book(Enchantment enchantment, int level) {
		return net.minecraft.world.item.EnchantedBookItem.createForEnchantment(
				new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, level));
	}

	/**
	 * In a real anvil (survival rules: the test player has no instabuild):
	 * Unbreaking and Mending books apply, other books do not; a prismarine
	 * shard repairs; two worn pairs combine.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void anvilRules(GameTestHelper h) {
		Player player = h.makeMockPlayer();
		check(h, !player.getAbilities().instabuild, "the test player must use survival anvil rules");
		ItemStack withUnbreaking = anvil(player, goggles(), book(Enchantments.UNBREAKING, 3));
		check(h, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, withUnbreaking) == 3, "Unbreaking III should apply, got " + withUnbreaking);
		ItemStack withMending = anvil(player, goggles(), book(Enchantments.MENDING, 1));
		check(h, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, withMending) == 1, "Mending should apply, got " + withMending);
		for (Enchantment other : List.of(Enchantments.ALL_DAMAGE_PROTECTION, Enchantments.THORNS, Enchantments.RESPIRATION)) {
			ItemStack result = anvil(player, goggles(), book(other, 1));
			check(h, result.isEmpty(), other.getDescriptionId() + " book must give no result, got " + result);
		}
		ItemStack worn = goggles();
		worn.setDamageValue(600);
		ItemStack repaired = anvil(player, worn, new ItemStack(Items.PRISMARINE_SHARD, 4));
		check(h, repaired.is(ModItems.SECRET_GOGGLES) && repaired.getDamageValue() < 600, "a prismarine shard should repair them, got " + repaired);
		ItemStack a = goggles();
		a.setDamageValue(600);
		ItemStack b = goggles();
		b.setDamageValue(600);
		ItemStack combined = anvil(player, a, b);
		check(h, combined.is(ModItems.SECRET_GOGGLES) && combined.getDamageValue() < 600, "two worn pairs should combine in the anvil, got " + combined);
		h.succeed();
	}

	/** Only Unbreaking and Mending apply; nothing comes from the enchanting table. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyUnbreakingAndMending(GameTestHelper h) {
		ItemStack stack = goggles();
		check(h, Enchantments.UNBREAKING.canEnchant(stack), "Unbreaking should apply");
		check(h, Enchantments.MENDING.canEnchant(stack), "Mending should apply");
		for (Enchantment other : List.of(Enchantments.ALL_DAMAGE_PROTECTION, Enchantments.PROJECTILE_PROTECTION,
				Enchantments.RESPIRATION, Enchantments.AQUA_AFFINITY, Enchantments.THORNS,
				Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE)) {
			check(h, !other.canEnchant(stack), other.getDescriptionId() + " must not apply to the goggles");
		}
		check(h, ModItems.SECRET_GOGGLES.getEnchantmentValue() == 0, "enchantability should be 0");
		check(h, EnchantmentHelper.selectEnchantment(RandomSource.create(1), goggles(), 30, true).isEmpty(),
				"the enchanting table should offer nothing");
		h.succeed();
	}

	/** End City treasure: about 10% of chests hold a pair (400 rolls → about 40). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void foundInEndCityTreasure(GameTestHelper h) {
		LootTable table = h.getLevel().getServer().getLootTables().get(BuiltInLootTables.END_CITY_TREASURE);
		int withGoggles = 0;
		for (int roll = 0; roll < 400; roll++) {
			LootContext context = new LootContext.Builder(h.getLevel())
					.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
					.withRandom(RandomSource.create(roll))
					.create(LootContextParamSets.CHEST);
			if (table.getRandomItems(context).stream().anyMatch(stack -> stack.is(ModItems.SECRET_GOGGLES))) {
				withGoggles++;
			}
		}
		check(h, withGoggles >= 20 && withGoggles <= 65, "expected about 40 of 400 chests with goggles, got " + withGoggles);
		h.succeed();
	}
}
