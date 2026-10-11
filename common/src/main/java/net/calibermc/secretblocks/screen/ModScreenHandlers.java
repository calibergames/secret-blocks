package net.calibermc.secretblocks.screen;

import net.calibermc.secretblocks.platform.Services;
import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.screen.SecretChestScreenHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;


public class ModScreenHandlers {

/*
    //SECRET CHEST
    public static ScreenHandlerType<SecretChestScreenHandler> SECRET_CHEST_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerSimple(new Identifier(SecretBlocks.MOD_ID, "secret_chest"),
                    SecretChestScreenHandler::new);
*/
    public static MenuType<SecretChestScreenHandler> SECRET_CHEST_SCREEN_HANDLER =
            new MenuType<>(SecretChestScreenHandler::new, FeatureFlags.VANILLA_SET);

    public static void registerAllScreenHandlers() {
        Services.PLATFORM.register(BuiltInRegistries.MENU, new ResourceLocation(SecretBlocks.MOD_ID, "secret_chest"),
                SECRET_CHEST_SCREEN_HANDLER);
    }
}

