package net.calibermc.secretblocks.registry;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.items.BuildersGoggles;
import net.calibermc.secretblocks.items.CamouflageWand;
import net.calibermc.secretblocks.items.SecretGoggles;
import net.calibermc.secretblocks.items.SwitchProbe;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ModItems {

    // Items
    public static final Item SECRET_GOGGLES = registerItem("secret_goggles",
            new SecretGoggles(new FabricItemSettings().rarity(Rarity.UNCOMMON)));

    // A builder's tool: creative inventory and /give only, no recipe or loot.
    public static final Item BUILDERS_GOGGLES = registerItem("builders_goggles",
            new BuildersGoggles(new FabricItemSettings().rarity(Rarity.EPIC)));

    // A builder's tool: creative inventory and /give only, no recipe or loot;
    // it also checks on the server that the player is in creative or an operator.
    public static final Item CAMOUFLAGE_WAND = registerItem("camouflage_wand",
            new CamouflageWand(new FabricItemSettings().rarity(Rarity.EPIC).stacksTo(1)));

    public static final Item SWITCH_PROBE = registerItem("switch_probe",
            new SwitchProbe( new FabricItemSettings().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item SWITCH_PROBE_ROTATION_MODE = registerItem("switch_probe_rotation_mode",
            new SwitchProbe( new FabricItemSettings().rarity(Rarity.RARE).stacksTo(1)));

    public static final Item CAMOUFLAGE_PASTE = registerItem("camouflage_paste",
            new Item( new FabricItemSettings()));


    // Registry
    private static Item registerItem(String name, Item item) {
        return ModCreativeTab.add(Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(SecretBlocks.MOD_ID, name), item));
    }

    // Console Output
    public static void registerItems() {
        System.out.println("Registering Items for " + SecretBlocks.MOD_ID);
    }
}

