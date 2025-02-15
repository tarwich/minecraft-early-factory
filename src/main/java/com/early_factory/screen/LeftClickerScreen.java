package com.early_factory.screen;

import javax.annotation.Nonnull;

import com.early_factory.EarlyFactory;
import com.early_factory.menu.LeftClickerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class LeftClickerScreen extends AbstractContainerScreen<LeftClickerMenu> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(EarlyFactory.MODID,
      "textures/gui/left_clicker_gui.png");

  public LeftClickerScreen(LeftClickerMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
  }

  @Override
  protected void renderBg(@Nonnull PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    int x = (width - imageWidth) / 2;
    int y = (height - imageHeight) / 2;

    blit(pPoseStack, x, y, 0, 0, imageWidth, imageHeight);
  }

  @Override
  public void render(@Nonnull PoseStack pPoseStack, int mouseX, int mouseY, float delta) {
    renderBackground(pPoseStack);
    super.render(pPoseStack, mouseX, mouseY, delta);
    renderTooltip(pPoseStack, mouseX, mouseY);
  }
}
