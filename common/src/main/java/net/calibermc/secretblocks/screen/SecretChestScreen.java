package net.calibermc.secretblocks.screen;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
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

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");
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
        graphics.blit(RenderType::guiTextured, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, ROWS * 18 + 17, 256, 256);
        graphics.blit(RenderType::guiTextured, TEXTURE, x, y + ROWS * 18 + 17, 0.0F, 126.0F, imageWidth, 96, 256, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
