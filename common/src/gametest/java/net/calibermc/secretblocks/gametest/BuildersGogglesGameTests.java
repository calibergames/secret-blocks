package net.calibermc.secretblocks.gametest;

import net.calibermc.secretblocks.items.BuildersGoggles;
import net.calibermc.secretblocks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import static net.calibermc.secretblocks.gametest.TestSupport.*;

/**
 * The Builder's Goggles' item. The builder's view itself is client code,
 * checked in the client.
 */
public class BuildersGogglesGameTests {

	private static ItemStack goggles() {
		return new ItemStack(ModItems.BUILDERS_GOGGLES);
	}

	/** Helmet slot, equipped by using them; no armour, toughness or other modifiers; worn with the Secret Goggles' look. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void wornOnTheHeadWithNoArmor(GameTestHelper h) {
		Player player = h.makeMockPlayer();
		player.setItemInHand(InteractionHand.MAIN_HAND, goggles());
		player.getItemInHand(InteractionHand.MAIN_HAND).use(h.getLevel(), player, InteractionHand.MAIN_HAND);
		check(h, player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BUILDERS_GOGGLES), "using them should put them on the head");
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			check(h, goggles().getAttributeModifiers(slot).isEmpty(), "no modifiers in " + slot + ", got " + goggles().getAttributeModifiers(slot));
		}
		check(h, player.getArmorValue() == 0, "armor value should be 0, is " + player.getArmorValue());
		check(h, ((ArmorItem) ModItems.BUILDERS_GOGGLES).getMaterial().getName().equals("secretblocks_goggles"),
				"worn, they should use the Secret Goggles' armour texture");
		h.succeed();
	}

	/** They never wear out: not damageable, and taking hits while wearing them leaves them as new. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void neverWearOut(GameTestHelper h) {
		check(h, !goggles().isDamageableItem(), "they must not be damageable (max damage " + goggles().getMaxDamage() + ")");
		Player player = h.makeMockPlayer();
		player.setItemSlot(EquipmentSlot.HEAD, goggles());
		for (int hit = 0; hit < 20; hit++) {
			player.invulnerableTime = 0;
			player.hurt(player.damageSources().generic(), 1.0f);
			player.setHealth(player.getMaxHealth());
		}
		for (int tick = 0; tick < 200; tick++) {
			player.tickCount = tick;
			player.getItemBySlot(EquipmentSlot.HEAD).inventoryTick(h.getLevel(), player, 39, false);
		}
		ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
		check(h, worn.is(ModItems.BUILDERS_GOGGLES) && worn.getDamageValue() == 0, "after 20 hits and 200 ticks they should be as new, got " + worn);
		h.succeed();
	}

	/**
	 * Not enchantable: the table offers nothing, no enchantment applies (as a
	 * helmet they would take Protection, Thorns, Respiration and the curses
	 * from books or {@code /enchant}), an anvil with a book gives nothing;
	 * and nothing repairs them.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void notEnchantableOrRepairable(GameTestHelper h) {
		check(h, ModItems.BUILDERS_GOGGLES.getEnchantmentValue() == 0, "enchantability should be 0");
		check(h, EnchantmentHelper.selectEnchantment(RandomSource.create(1), goggles(), 30, true).isEmpty(),
				"the enchanting table should offer nothing");
		for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
			check(h, !enchantment.canEnchant(goggles()), enchantment.getDescriptionId() + " must not apply to them");
		}
		Player player = h.makeMockPlayer();
		AnvilMenu anvil = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		anvil.getSlot(0).set(goggles());
		anvil.getSlot(1).set(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.ALL_DAMAGE_PROTECTION, 1)));
		anvil.createResult();
		check(h, anvil.getSlot(2).getItem().isEmpty(), "an anvil with a Protection book must give nothing, got " + anvil.getSlot(2).getItem());
		for (ItemStack material : new ItemStack[] {new ItemStack(Items.PRISMARINE_SHARD), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.LEATHER), goggles()}) {
			check(h, !ModItems.BUILDERS_GOGGLES.isValidRepairItem(goggles(), material), material + " must not repair them");
		}
		h.succeed();
	}

	/** A creative/op tool: no recipe makes them and no loot table holds them. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void noRecipeOrLoot(GameTestHelper h) {
		MinecraftServer server = h.getLevel().getServer();
		check(h, server.getRecipeManager().getRecipes().stream().noneMatch(r -> r.getResultItem(server.registryAccess()).is(ModItems.BUILDERS_GOGGLES)),
				"no recipe may make them");
		int tables = 0;
		for (ResourceLocation id : server.getLootData().getKeys(net.minecraft.world.level.storage.loot.LootDataType.TABLE)) {
			LootTable table = server.getLootData().getLootTable(id);
			if (table.getParamSet() != LootContextParamSets.CHEST) {
				continue;
			}
			tables++;
			for (int roll = 0; roll < 20; roll++) {
				net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
						.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
						.create(LootContextParamSets.CHEST);
				check(h, table.getRandomItems(params, roll).stream().noneMatch(stack -> stack.is(ModItems.BUILDERS_GOGGLES)),
						"chest loot " + id + " must not hold them");
			}
		}
		check(h, tables >= 20, "expected vanilla's chest loot tables, found " + tables);
		check(h, BuiltInRegistries.ITEM.getKey(ModItems.BUILDERS_GOGGLES).getPath().equals("builders_goggles"), "registered as secretblocks:builders_goggles");
		check(h, ModItems.BUILDERS_GOGGLES instanceof BuildersGoggles, "the Builder's Goggles item");
		h.succeed();
	}
}
