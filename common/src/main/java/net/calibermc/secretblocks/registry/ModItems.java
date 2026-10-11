package net.calibermc.secretblocks.registry;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.items.BuildersGoggles;
import net.calibermc.secretblocks.items.CamouflageWand;
import net.calibermc.secretblocks.items.SecretGoggles;
import net.calibermc.secretblocks.items.SwitchProbe;
import net.calibermc.secretblocks.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ModItems {

    // Items
    public static final Item SECRET_GOGGLES = registerItem("secret_goggles",
            new SecretGoggles(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // A builder's tool: creative inventory and /give only, no recipe or loot.
    public static final Item BUILDERS_GOGGLES = registerItem("builders_goggles",
            new BuildersGoggles(new Item.Properties().rarity(Rarity.EPIC)));

    // A builder's tool: creative inventory and /give only, no recipe or loot;
    // it also checks on the server that the player is in creative or an operator.
    public static final Item CAMOUFLAGE_WAND = registerItem("camouflage_wand",
            new CamouflageWand(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final Item SWITCH_PROBE = registerItem("switch_probe",
            new SwitchProbe( new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item SWITCH_PROBE_ROTATION_MODE = registerItem("switch_probe_rotation_mode",
            new SwitchProbe( new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item CAMOUFLAGE_PASTE = registerItem("camouflage_paste",
            new Item( new Item.Properties()));


    // Registry
    private static Item registerItem(String name, Item item) {
        return ModCreativeTab.add(Services.PLATFORM.register(BuiltInRegistries.ITEM, new ResourceLocation(SecretBlocks.MOD_ID, name), item));
    }

    // Console Output
    public static void registerItems() {
        System.out.println("Registering Items for " + SecretBlocks.MOD_ID);
    }
}

