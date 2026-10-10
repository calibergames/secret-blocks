package net.calibermc.secretblocks.screen;

import net.calibermc.secretblocks.SecretBlocks;
import net.calibermc.secretblocks.screen.SecretChestScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;


public class ModScreenHandlers {

/*
    //SECRET CHEST
    public static ScreenHandlerType<SecretChestScreenHandler> SECRET_CHEST_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerSimple(new Identifier(SecretBlocks.MOD_ID, "secret_chest"),
                    SecretChestScreenHandler::new);
*/
    public static MenuType<SecretChestScreenHandler> SECRET_CHEST_SCREEN_HANDLER =
            new ExtendedScreenHandlerType<>(SecretChestScreenHandler::new);

    public static void registerAllScreenHandlers() {
        Registry.register(BuiltInRegistries.MENU, new ResourceLocation(SecretBlocks.MOD_ID, "secret_chest"),
                SECRET_CHEST_SCREEN_HANDLER);
    }
}

