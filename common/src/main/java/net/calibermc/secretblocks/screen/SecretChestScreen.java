package net.calibermc.secretblocks.screen;


import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Secret Chest's screen: drawn as vanilla's single chest is, the chest
 * texture's top three rows and its inventory part, with the title on the
 * left.
 */
public class SecretChestScreen extends AbstractContainerScreen<SecretChestScreenHandler> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
    private static final int ROWS = 3;

    public SecretChestScreen(SecretChestScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 176, 114 + ROWS * 18);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, ROWS * 18 + 17, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y + ROWS * 18 + 17, 0.0F, 126.0F, imageWidth, 96, 256, 256);
    }
}
