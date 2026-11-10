package net.calibermc.secretblocks.gametest;

import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import net.calibermc.secretblocks.items.SecretGoggles;
import net.calibermc.secretblocks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.calibermc.secretblocks.gametest.support.GameTest;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
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
public class GogglesGameTests {

	private static ItemStack goggles() {
		return new ItemStack(ModItems.SECRET_GOGGLES);
	}

	/** A test player wearing {@code stack}, at the given wear-check tick. */
	private static Player wearer(GameTestHelper h, ItemStack stack) {
		Player player = h.makeMockPlayer(GameType.CREATIVE);
		player.setItemSlot(EquipmentSlot.HEAD, stack);
		return player;
	}

	/** Worn: about 20% of once-a-second checks cost 1 durability (1,000 checks → about 200). */
	@GameTest
	public void wearsOutWhileWorn(GameTestHelper h) {
		ItemStack stack = goggles();
		Player player = wearer(h, stack);
		for (int check = 0; check < 1000; check++) {
			player.tickCount = check * SecretGoggles.WEAR_INTERVAL_TICKS;
			stack.inventoryTick(h.getLevel(), player, EquipmentSlot.HEAD);
		}
		int lost = stack.getDamageValue();
		check(h, lost >= 150 && lost <= 250, "expected about 200 durability lost in 1,000 checks, lost " + lost);
		check(h, stack.getMaxDamage() == SecretGoggles.DURABILITY, "durability should be 900, is " + stack.getMaxDamage());
		h.succeed();
	}

	/** Between the once-a-second checks, and when not worn, nothing is lost. */
	@GameTest
	public void noWearOffTheSecondOrWhenNotWorn(GameTestHelper h) {
		ItemStack worn = goggles();
		Player player = wearer(h, worn);
		for (int tick = 1; tick < 2000; tick++) {
			if (tick % SecretGoggles.WEAR_INTERVAL_TICKS != 0) {
				player.tickCount = tick;
				worn.inventoryTick(h.getLevel(), player, EquipmentSlot.HEAD);
			}
		}
		check(h, worn.getDamageValue() == 0, "no wear between checks, lost " + worn.getDamageValue());
		ItemStack carried = goggles();
		for (int check = 0; check < 500; check++) {
			player.tickCount = check * SecretGoggles.WEAR_INTERVAL_TICKS;
			carried.inventoryTick(h.getLevel(), player, null);
		}
		check(h, carried.getDamageValue() == 0, "goggles carried in the inventory must not wear, lost " + carried.getDamageValue());
		h.succeed();
	}

	/** At 0 durability they break and leave the head slot. */
	@GameTest
	public void breakAtZero(GameTestHelper h) {
		ItemStack stack = goggles();
		stack.setDamageValue(stack.getMaxDamage() - 1);
		Player player = wearer(h, stack);
		for (int check = 0; check < 500 && !player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(); check++) {
			player.tickCount = check * SecretGoggles.WEAR_INTERVAL_TICKS;
			stack.inventoryTick(h.getLevel(), player, EquipmentSlot.HEAD);
		}
		check(h, player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() || stack.isEmpty(), "the goggles should have broken");
		h.succeed();
	}

	/** Helmet slot, no armor value, equipped by using them. */
	@GameTest
	public void wornOnTheHeadWithNoArmor(GameTestHelper h) {
		Player player = h.makeMockPlayer(GameType.CREATIVE);
		player.setItemInHand(InteractionHand.MAIN_HAND, goggles());
		player.getItemInHand(InteractionHand.MAIN_HAND).use(h.getLevel(), player, InteractionHand.MAIN_HAND);
		check(h, player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.SECRET_GOGGLES), "using them should put them on the head");
		check(h, goggles().getItem().components().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().isEmpty()
				&& goggles().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).modifiers().isEmpty(), "no armor or other modifiers");
		check(h, player.getArmorValue() == 0, "armor value should be 0, is " + player.getArmorValue());
		h.succeed();
	}

	/** Repair: prismarine shard is the repair item; two worn pairs combine in a crafting grid (the anvil is tested below). */
	@GameTest
	public void repairedWithPrismarineOrAnotherPair(GameTestHelper h) {
		check(h, repairs(goggles(), new ItemStack(Items.PRISMARINE_SHARD)), "prismarine shard repairs them");
		check(h, !repairs(goggles(), new ItemStack(Items.IRON_INGOT)), "iron does not");
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

	private static ItemStack book(Holder<Enchantment> enchantment, int level) {
		return net.minecraft.world.item.enchantment.EnchantmentHelper.createBook(
				new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, level));
	}

	/** The enchantment registered under {@code key} in this world (enchantments are data in 1.21). */
	private static Holder<Enchantment> enchantment(GameTestHelper h, ResourceKey<Enchantment> key) {
		return h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	/**
	 * In a real anvil (survival rules: the test player has no instabuild):
	 * Unbreaking and Mending books apply, armour books do not; a prismarine
	 * shard repairs; two worn pairs combine.
	 */
	@GameTest
	public void anvilRules(GameTestHelper h) {
		Player player = h.makeMockPlayer(GameType.CREATIVE);
		check(h, !player.getAbilities().instabuild, "the test player must use survival anvil rules");
		Holder<Enchantment> unbreaking = enchantment(h, Enchantments.UNBREAKING);
		ItemStack withUnbreaking = anvil(player, goggles(), book(unbreaking, 3));
		check(h, EnchantmentHelper.getItemEnchantmentLevel(unbreaking, withUnbreaking) == 3, "Unbreaking III should apply, got " + withUnbreaking);
		Holder<Enchantment> mending = enchantment(h, Enchantments.MENDING);
		ItemStack withMending = anvil(player, goggles(), book(mending, 1));
		check(h, EnchantmentHelper.getItemEnchantmentLevel(mending, withMending) == 1, "Mending should apply, got " + withMending);
		for (ResourceKey<Enchantment> other : List.of(Enchantments.PROTECTION, Enchantments.THORNS, Enchantments.RESPIRATION)) {
			ItemStack result = anvil(player, goggles(), book(enchantment(h, other), 1));
			check(h, result.isEmpty(), other.location() + " book must give no result, got " + result);
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

	/**
	 * The enchantments for durability items apply (Unbreaking, Mending and, as
	 * on every such item, Curse of Vanishing), no armour enchantment does, and
	 * nothing comes from the enchanting table.
	 */
	@GameTest
	public void onlyTheDurabilityEnchantments(GameTestHelper h) {
		ItemStack stack = goggles();
		for (ResourceKey<Enchantment> applies : List.of(Enchantments.UNBREAKING, Enchantments.MENDING, Enchantments.VANISHING_CURSE)) {
			check(h, enchantment(h, applies).value().canEnchant(stack), applies.location() + " should apply to the goggles");
		}
		for (ResourceKey<Enchantment> other : List.of(Enchantments.PROTECTION, Enchantments.PROJECTILE_PROTECTION,
				Enchantments.RESPIRATION, Enchantments.AQUA_AFFINITY, Enchantments.THORNS, Enchantments.BINDING_CURSE)) {
			check(h, !enchantment(h, other).value().canEnchant(stack), other.location() + " must not apply to the goggles");
		}
		check(h, !goggles().has(DataComponents.ENCHANTABLE), "they should not be enchantable at a table");
		Registry<Enchantment> all = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		check(h, EnchantmentHelper.selectEnchantment(RandomSource.create(1), goggles(), 30, all.listElements().map(e -> (Holder<Enchantment>) e)).isEmpty(),
				"the enchanting table should offer nothing");
		h.succeed();
	}

	/** End City treasure: about 10% of chests hold a pair (400 rolls → about 40). */
	@GameTest
	public void foundInEndCityTreasure(GameTestHelper h) {
		LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.END_CITY_TREASURE);
		int withGoggles = 0;
		for (int roll = 0; roll < 400; roll++) {
			net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
					.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
					.create(LootContextParamSets.CHEST);
			if (table.getRandomItems(params, roll).stream().anyMatch(stack -> stack.is(ModItems.SECRET_GOGGLES))) {
				withGoggles++;
			}
		}
		check(h, withGoggles >= 20 && withGoggles <= 65, "expected about 40 of 400 chests with goggles, got " + withGoggles);
		h.succeed();
	}
}
