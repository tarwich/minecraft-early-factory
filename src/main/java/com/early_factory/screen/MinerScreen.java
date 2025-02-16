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

import com.early_factory.util.MiningTiers;

public class MinerScreen extends AbstractContainerScreen<MinerMenu> {
  private static final ResourceLocation TEXTURE = new ResourceLocation(EarlyFactory.MOD_ID,
      "textures/gui/miner_gui.png");
  private static final DecimalFormat DEPTH_FORMAT = new DecimalFormat("#");
  private static final int GREEN_COLOR = 0x409630;
  private float scrollOffset = 0;
  private boolean isScrolling = false;
  private List<ResourceLocation> displayedBlocks = new ArrayList<>();
  private static final int SCROLL_HEIGHT = 54; // Reduced to match 3 rows height
  private static final int ITEMS_PER_ROW = 3;
  private static final int VISIBLE_ROWS = 3;
  private static final int ITEM_SPACING = 18; // Space between items
  private static final int LIST_X = 106; // X position of the list
  private static final int LIST_Y = 17; // Y position of the list
  private static final int MINEABLE_BORDER_COLOR = 0xFF00FF00; // Green border
  private static final int UNMINEABLE_OVERLAY_COLOR = 0x80FF0000; // Semi-transparent red

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

      // Convert to list of entries and sort by quantity in descending order
      List<Map.Entry<ResourceLocation, Integer>> sortedEntries = new ArrayList<>(scannedBlocks.entrySet());
      sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

      // Add sorted blocks to displayedBlocks
      for (Map.Entry<ResourceLocation, Integer> entry : sortedEntries) {
        displayedBlocks.add(entry.getKey());
      }
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
    if (isScrollBarActive() && mouseX >= leftPos + 159 && mouseX < leftPos + 171
        && mouseY >= topPos + 17 && mouseY < topPos + 71) {
      isScrolling = true;
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (isScrolling) {
      int maxScroll = ((displayedBlocks.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW - VISIBLE_ROWS);
      float dragAmount = (float) (mouseY - (topPos + 17)) / (54.0f - 15);
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
    // Update the displayed blocks each render frame
    updateDisplayedBlocks();

    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, TEXTURE);
    int x = (width - imageWidth) / 2;
    int y = (height - imageHeight) / 2;

    blit(pPoseStack, x, y, 0, 0, imageWidth, imageHeight);

    // Draw scroll bar
    if (isScrollBarActive()) {
      float scrollBarHeight = 54;
      int maxScroll = ((displayedBlocks.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW - VISIBLE_ROWS);
      float scrollBarY = topPos + 17 + ((scrollBarHeight - 15) * (scrollOffset / maxScroll));
      blit(pPoseStack, leftPos + 159, (int) scrollBarY, 178, 0, 12, 15);
    }

    // Draw items
    int firstRow = (int) scrollOffset;
    int lastRow = Math.min(firstRow + VISIBLE_ROWS, (displayedBlocks.size() + ITEMS_PER_ROW - 1) / ITEMS_PER_ROW);

    // Get pipe stack before the loop
    ItemStack pipeStack = menu.getSlot(MinerMenu.PIPE_SLOT_INDEX).getItem();

    for (int row = firstRow; row < lastRow; row++) {
      for (int col = 0; col < ITEMS_PER_ROW; col++) {
        int index = row * ITEMS_PER_ROW + col;
        if (index < displayedBlocks.size()) {
          ResourceLocation blockId = displayedBlocks.get(index);
          Block block = Registry.BLOCK.get(blockId);
          ItemStack stack = new ItemStack(block.asItem());

          int itemX = leftPos + LIST_X + col * ITEM_SPACING;
          int itemY = topPos + LIST_Y + (row - firstRow) * ITEM_SPACING;

          // Render the item
          itemRenderer.renderGuiItem(stack, itemX, itemY);

          // Draw mineable indicator
          boolean canMine = canMineBlock(block, pipeStack);
          if (!pipeStack.isEmpty()) {
            if (canMine) {
              // Draw green border around mineable items
              fill(pPoseStack, itemX - 1, itemY - 1, itemX + 17, itemY, MINEABLE_BORDER_COLOR); // Top
              fill(pPoseStack, itemX - 1, itemY + 16, itemX + 17, itemY + 17, MINEABLE_BORDER_COLOR); // Bottom
              fill(pPoseStack, itemX - 1, itemY, itemX, itemY + 16, MINEABLE_BORDER_COLOR); // Left
              fill(pPoseStack, itemX + 16, itemY, itemX + 17, itemY + 16, MINEABLE_BORDER_COLOR); // Right
            } else {
              // Draw red overlay on unmineable items
              fill(pPoseStack, itemX, itemY, itemX + 16, itemY + 16, UNMINEABLE_OVERLAY_COLOR);
            }
          }

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

  private boolean canMineBlock(Block block, ItemStack pipeStack) {
    return MiningTiers.canMineBlock(block, pipeStack);
  }

  private int getBlockHardnessTier(Block block) {
    String blockId = Registry.BLOCK.getKey(block).toString();
    // Diamond-tier blocks
    if (blockId.contains("obsidian") || blockId.contains("diamond") ||
        blockId.contains("ancient_debris"))
      return 4;
    // Iron-tier blocks
    if (blockId.contains("gold") || blockId.contains("iron") ||
        blockId.contains("emerald") || blockId.contains("lapis"))
      return 3;
    // Stone-tier blocks
    if (blockId.contains("stone") || blockId.contains("copper") ||
        blockId.contains("coal"))
      return 2;
    // Wooden-tier blocks (dirt, gravel, sand etc)
    return 1;
  }
}
