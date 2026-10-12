package net.calibermc.secretblocks;

import net.calibermc.secretblocks.items.CamouflageWand;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModCreativeTab;
import net.calibermc.secretblocks.registry.ModEntities;
import net.calibermc.secretblocks.registry.ModItems;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.calibermc.secretblocks.blocks.Opacity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's loader-neutral start: each loader's entry point calls {@link #init}
 * and wires its own events to the hooks named there.
 */
public class SecretBlocks {

    public static final String MOD_ID = "secretblocks";

    public static ResourceLocation id(String id) {
        return new ResourceLocation("secretblocks", id);
    }

    /** Whether {@link #gogglesLootPool} goes into this loot table: vanilla's End City treasure. */
    public static boolean getsGogglesLoot(ResourceLocation lootTable) {
        return BuiltInLootTables.END_CITY_TREASURE.equals(lootTable);
    }

    /**
     * The pool End City treasure chests get: the Secret Goggles at 10% per
     * chest (as in the original). Loaders add it to the table
     * {@link #getsGogglesLoot} names when vanilla's table loads, not to a data
     * pack's replacement.
     */
    public static LootPool.Builder gogglesLootPool() {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModItems.SECRET_GOGGLES))
                .when(LootItemRandomChanceCondition.randomChance(0.1f));
    }

    /**
     * Registers the mod's blocks, items, block entities, creative tab and menu.
     * Each loader also hooks up:
     * <ul>
     * <li>{@link CamouflageWand#onUseBlock} before a clicked block's own use;</li>
     * <li>{@link #gogglesLootPool} into vanilla's End City treasure;</li>
     * <li>{@link Opacity}'s chunk-load, end-of-level-tick and level-unload
     * hooks, so older secret blocks learn whether their looks let light
     * through.</li>
     * </ul>
     */
    public static void init() {
        ModBlocks.registerBlocks();
        ModItems.registerItems();
        ModCreativeTab.register();
        ModEntities.registerAllEntities();
        ModScreenHandlers.registerAllScreenHandlers();
    }
}
