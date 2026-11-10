package net.calibermc.secretblocks.registry;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.items.BuildersGoggles;
import net.calibermc.secretblocks.items.CamouflageWand;
import net.calibermc.secretblocks.items.SecretGoggles;
import net.calibermc.secretblocks.items.SwitchProbe;
import net.calibermc.secretblocks.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.function.Function;

public class ModItems {

    // Items
    public static final Item SECRET_GOGGLES = registerItem("secret_goggles", SecretGoggles::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));

    // A builder's tool: creative inventory and /give only, no recipe or loot.
    public static final Item BUILDERS_GOGGLES = registerItem("builders_goggles", BuildersGoggles::new,
            new Item.Properties().rarity(Rarity.EPIC));

    // A builder's tool: creative inventory and /give only, no recipe or loot;
    // it also checks on the server that the player is in creative or an operator.
    public static final Item CAMOUFLAGE_WAND = registerItem("camouflage_wand", CamouflageWand::new,
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    public static final Item SWITCH_PROBE = registerItem("switch_probe", SwitchProbe::new,
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));

    public static final Item SWITCH_PROBE_ROTATION_MODE = registerItem("switch_probe_rotation_mode", SwitchProbe::new,
            new Item.Properties().rarity(Rarity.RARE).stacksTo(1));

    public static final Item CAMOUFLAGE_PASTE = registerItem("camouflage_paste", Item::new,
            new Item.Properties());


    // Registry
    // Items are built with their registry key already in their properties (see ModBlocks).
    private static Item registerItem(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Identifier id = SecretBlocks.id(name);
        Item item = factory.apply(properties.setId(ResourceKey.create(Registries.ITEM, id)));
        return ModCreativeTab.add(Services.PLATFORM.register(BuiltInRegistries.ITEM, id, item));
    }

    // Console Output
    public static void registerItems() {
        System.out.println("Registering Items for " + SecretBlocks.MOD_ID);
    }
}

