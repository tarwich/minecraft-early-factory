package com.early_factory.screen;

import com.early_factory.EarlyFactory;
import com.early_factory.menu.CollectorMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CollectorScreen extends AbstractContainerScreen<CollectorMenu> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(EarlyFactory.MOD_ID,
          "textures/gui/collector.png");

  public CollectorScreen(CollectorMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    this.imageWidth = 176;
    this.imageHeight = 166;
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
