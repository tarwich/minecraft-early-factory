package com.early_factory.screen;

import javax.annotation.Nonnull;

import com.early_factory.EarlyFactory;
import com.early_factory.menu.MinerMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.renderer.entity.ItemRenderer;
import com.early_factory.block.entity.MinerBlockEntity;
import java.text.DecimalFormat;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinerScreen extends AbstractContainerScreen<MinerMenu> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(EarlyFactory.MOD_ID,
      "textures/gui/miner_gui.png");
  private static final DecimalFormat DEPTH_FORMAT = new DecimalFormat("#");
  private static final int GREEN_COLOR = 0x409630;
  private float scrollOffset = 0;
  private boolean isScrolling = false;
  private List<ResourceLocation> displayedBlocks = new ArrayList<>();
  private static final int SCROLL_HEIGHT = 90; // Height of scrollable area
  private static final int ITEMS_PER_ROW = 3;
  private static final int VISIBLE_ROWS = 5;
  private static final int ITEM_SPACING = 18; // Space between items
  private static final int LIST_X = 98; // X position of the list
  private static final int LIST_Y = 17; // Y position of the list

  public MinerScreen(MinerMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    this.imageWidth = 176;
    this.imageHeight = 166;
  }

  @Override
  protected void init() {
    super.init();
    updateDisplayedBlocks();
  }

  private void updateDisplayedBlocks() {
    displayedBlocks.clear();
    BlockEntity blockEntity = menu.getBlockEntity();
    if (blockEntity instanceof MinerBlockEntity) {
      MinerBlockEntity miner = (MinerBlockEntity) blockEntity;
      Map<ResourceLocation, Integer> scannedBlocks = miner.getScannedBlocks();
      displayedBlocks.addAll(scannedBlocks.keySet());
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (displayedBlocks.size() > ITEMS_PER_ROW * VISIBLE_ROWS) {
      int maxScroll = ((displayedBlocks.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW - VISIBLE_ROWS);
      scrollOffset = (float) Math.max(0, Math.min(maxScroll, scrollOffset - delta * 0.25f));
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (isScrollBarActive() && mouseX >= leftPos + 166 && mouseX < leftPos + 176
        && mouseY >= topPos + 17 && mouseY < topPos + 107) {
      isScrolling = true;
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (isScrolling) {
      int maxScroll = ((displayedBlocks.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW - VISIBLE_ROWS);
      float dragAmount = (float) (mouseY - (topPos + 17)) / SCROLL_HEIGHT;
      scrollOffset = Math.max(0, Math.min(maxScroll, dragAmount * maxScroll));
      return true;
    }
    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    isScrolling = false;
    return super.mouseReleased(mouseX, mouseY, button);
  }

  private boolean isScrollBarActive() {
    return displayedBlocks.size() > ITEMS_PER_ROW * VISIBLE_ROWS;
  }

  @Override
  protected void renderBg(@Nonnull PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    int x = (width - imageWidth) / 2;
    int y = (height - imageHeight) / 2;

    blit(pPoseStack, x, y, 0, 0, imageWidth, imageHeight);

    // Draw scroll bar
    if (isScrollBarActive()) {
      float scrollBarHeight = 90;
      int maxScroll = ((displayedBlocks.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW - VISIBLE_ROWS);
      float scrollBarY = topPos + 17 + (scrollBarHeight * (scrollOffset / maxScroll));
      blit(pPoseStack, leftPos + 166, (int) scrollBarY, 176, 0, 10, 15);
    }

    // Draw items
    int firstRow = (int) scrollOffset;
    int lastRow = Math.min(firstRow + VISIBLE_ROWS, (displayedBlocks.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW);

    for (int row = firstRow; row < lastRow; row++) {
      for (int col = 0; col < ITEMS_PER_ROW; col++) {
        int index = row * ITEMS_PER_ROW + col;
        if (index < displayedBlocks.size()) {
          ResourceLocation blockId = displayedBlocks.get(index);
          Block block = Registry.BLOCK.get(blockId);
          ItemStack stack = new ItemStack(block.asItem());

          int itemX = leftPos + LIST_X + col * ITEM_SPACING;
          int itemY = topPos + LIST_Y + (row - firstRow) * ITEM_SPACING;

          itemRenderer.renderGuiItem(stack, itemX, itemY);

          // Draw quantity with new formatting
          BlockEntity blockEntity = menu.getBlockEntity();
          if (blockEntity instanceof MinerBlockEntity) {
            MinerBlockEntity miner = (MinerBlockEntity) blockEntity;
            Map<ResourceLocation, Integer> scannedBlocks = miner.getScannedBlocks();
            int quantity = scannedBlocks.getOrDefault(blockId, 0);
            if (quantity > 0) {
              String count = formatQuantity(quantity);
              itemRenderer.renderGuiItemDecorations(font, stack, itemX, itemY, count);
            }
          }
        }
      }
    }

    // Get pipe item from slot and current Y level
    ItemStack pipeStack = menu.getSlot(MinerMenu.PIPE_SLOT_INDEX).getItem();
    ItemStack pickaxe = getPickaxeForLevel(pipeStack);

    // Render pickaxe icon
    if (!pickaxe.isEmpty() && minecraft != null) {
      ItemRenderer itemRenderer = minecraft.getItemRenderer();
      itemRenderer.renderGuiItem(pickaxe, x + 28, y + 52);
    }
  }

  @Override
  protected void renderLabels(@Nonnull PoseStack poseStack, int mouseX, int mouseY) {
    super.renderLabels(poseStack, mouseX, mouseY);

    MinerBlockEntity blockEntity = menu.getBlockEntity();
    if (blockEntity != null) {
      int yLevel = blockEntity.getCurrentYLevel();
      String yLevelText = "Y-Level: " + DEPTH_FORMAT.format(yLevel);
      font.draw(poseStack, yLevelText, 10, 20, GREEN_COLOR);
    }

    // Add tooltips for items
    int relativeX = mouseX - leftPos;
    int relativeY = mouseY - topPos;

    if (relativeX >= LIST_X && relativeX < LIST_X + ITEMS_PER_ROW * ITEM_SPACING &&
        relativeY >= LIST_Y && relativeY < LIST_Y + VISIBLE_ROWS * ITEM_SPACING) {

      int col = (relativeX - LIST_X) / ITEM_SPACING;
      int row = (relativeY - LIST_Y) / ITEM_SPACING + (int) scrollOffset;
      int index = row * ITEMS_PER_ROW + col;

      if (index < displayedBlocks.size()) {
        ResourceLocation blockId = displayedBlocks.get(index);
        Block block = Registry.BLOCK.get(blockId);
        ItemStack stack = new ItemStack(block.asItem());

        if (!stack.isEmpty()) {
          renderTooltip(poseStack, stack, relativeX, relativeY);
        }
      }
    }
  }

  private ItemStack getPickaxeForLevel(ItemStack pipeStack) {
    if (pipeStack.isEmpty())
      return ItemStack.EMPTY;

    String itemName = pipeStack.getItem().toString().toLowerCase();
    if (itemName.contains("diamond"))
      return new ItemStack(Items.DIAMOND_PICKAXE);
    if (itemName.contains("iron"))
      return new ItemStack(Items.IRON_PICKAXE);
    if (itemName.contains("stone"))
      return new ItemStack(Items.STONE_PICKAXE);
    if (itemName.contains("wooden"))
      return new ItemStack(Items.WOODEN_PICKAXE);
    return ItemStack.EMPTY;
  }

  @Override
  public void render(@Nonnull PoseStack pPoseStack, int mouseX, int mouseY, float delta) {
    renderBackground(pPoseStack);
    super.render(pPoseStack, mouseX, mouseY, delta);
    renderTooltip(pPoseStack, mouseX, mouseY);
  }

  private String formatQuantity(int quantity) {
    if (quantity < 1000) {
      return String.valueOf(quantity);
    }
    return String.format("%.1fk", quantity / 1000.0);
  }
}
