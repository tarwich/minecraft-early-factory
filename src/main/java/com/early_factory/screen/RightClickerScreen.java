package com.early_factory.screen;

import com.early_factory.EarlyFactory;
import com.early_factory.menu.RightClickerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RightClickerScreen extends AbstractContainerScreen<RightClickerMenu> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(EarlyFactory.MOD_ID,
          "textures/gui/right_clicker.png");

  public RightClickerScreen(RightClickerMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
  }

  @Override
  protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    int x = (width - imageWidth) / 2;
    int y = (height - imageHeight) / 2;

    this.blit(poseStack, x, y, 0, 0, imageWidth, imageHeight);
  }

  @Override
  public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
    renderBackground(poseStack);
    super.render(poseStack, mouseX, mouseY, delta);
    renderTooltip(poseStack, mouseX, mouseY);
  }
}
