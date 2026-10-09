package net.calibermc.secretblocks;

import net.calibermc.secretblocks.items.CamouflageWand;
import net.calibermc.secretblocks.registry.ModBlocks;
import net.calibermc.secretblocks.registry.ModEntities;
import net.calibermc.secretblocks.registry.ModItems;
import net.calibermc.secretblocks.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class SecretBlocks implements ModInitializer {

    public static final String MOD_ID = "secretblocks";

    public static ResourceLocation id(String id) {
        return new ResourceLocation("secretblocks", id);
    }


    // CREATIVE INVENTORY TAB GROUP
    public static final CreativeModeTab SECRET_BLOCKS_GROUP = FabricItemGroupBuilder.create(id("secret_blocks")).icon(() -> new ItemStack(ModItems.CAMOUFLAGE_PASTE)).build();

    /** End City treasure chests: an extra pool with the Secret Goggles at 10% per chest (as in the original). */
    private static void registerGogglesLoot() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (source.isBuiltin() && BuiltInLootTables.END_CITY_TREASURE.equals(id)) {
                tableBuilder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(ModItems.SECRET_GOGGLES))
                        .when(LootItemRandomChanceCondition.randomChance(0.1f)));
            }
        });
    }

    @Override
    public void onInitialize() {

        ModBlocks.registerBlocks();
        ModItems.registerItems();
        ModEntities.registerAllEntities();
        ModScreenHandlers.registerAllScreenHandlers();
        registerGogglesLoot();
        // The wand acts before the clicked block's own use.
        UseBlockCallback.EVENT.register(CamouflageWand::onUseBlock);
        // Older secret blocks learn whether their looks let light through.
        net.calibermc.secretblocks.blocks.Opacity.register();

    }
}
