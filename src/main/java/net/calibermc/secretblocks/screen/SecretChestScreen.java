package net.calibermc.secretblocks.screen;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Secret Chest's screen: drawn as vanilla's single chest is, the chest
 * texture's top three rows and its inventory part, with the title on the
 * left.
 */
public class SecretChestScreen extends AbstractContainerScreen<SecretChestScreenHandler> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final int ROWS = 3;

    public SecretChestScreen(SecretChestScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        imageHeight = 114 + ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, ROWS * 18 + 17);
        graphics.blit(TEXTURE, x, y + ROWS * 18 + 17, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
