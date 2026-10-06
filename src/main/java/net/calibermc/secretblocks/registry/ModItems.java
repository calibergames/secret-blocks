package net.calibermc.secretblocks.registry;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.items.BuildersGoggles;
import net.calibermc.secretblocks.items.SecretGoggles;
import net.calibermc.secretblocks.items.SwitchProbe;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import static net.calibermc.secretblocks.SecretBlocks.SECRET_BLOCKS_GROUP;

public class ModItems {

    // Items
    public static final Item SECRET_GOGGLES = registerItem("secret_goggles",
            new SecretGoggles(new FabricItemSettings().tab(SECRET_BLOCKS_GROUP).rarity(Rarity.UNCOMMON)));

    // A builder's tool: creative inventory and /give only, no recipe or loot.
    public static final Item BUILDERS_GOGGLES = registerItem("builders_goggles",
            new BuildersGoggles(new FabricItemSettings().tab(SECRET_BLOCKS_GROUP).rarity(Rarity.EPIC)));

    public static final Item SWITCH_PROBE = registerItem("switch_probe",
            new SwitchProbe( new FabricItemSettings().tab(SECRET_BLOCKS_GROUP).rarity(Rarity.RARE).stacksTo(1)));

    public static final Item SWITCH_PROBE_ROTATION_MODE = registerItem("switch_probe_rotation_mode",
            new SwitchProbe( new FabricItemSettings().tab(SECRET_BLOCKS_GROUP).rarity(Rarity.RARE).stacksTo(1)));

    public static final Item CAMOUFLAGE_PASTE = registerItem("camouflage_paste",
            new Item( new FabricItemSettings().tab(SECRET_BLOCKS_GROUP)));


    // Registry
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, new ResourceLocation(SecretBlocks.MOD_ID, name), item);
    }

    // Console Output
    public static void registerItems() {
        System.out.println("Registering Items for " + SecretBlocks.MOD_ID);
    }
}

