package com.early_factory.menu;

import javax.annotation.Nonnull;

import com.early_factory.ModBlocks;
import com.early_factory.ModMenuTypes;
import com.early_factory.block.entity.MinerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;
import net.minecraftforge.items.IItemHandler;
import java.util.HashMap;
import java.util.Map;

public class MinerMenu extends AbstractContainerMenu {
  private final MinerBlockEntity blockEntity;
  public static final int PIPE_SLOT_INDEX = 36; // After player inventory
  public static final TagKey<Item> MINING_PIPES = TagKey.create(Registry.ITEM_REGISTRY,
      new ResourceLocation("early_factory", "mining_pipes"));

  private Map<ResourceLocation, Integer> scannedBlocks = new HashMap<>();

  // Custom slot class for mining pipes
  private static class MiningPipeSlot extends SlotItemHandler {
    public MiningPipeSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
      super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
      return stack.is(MINING_PIPES);
    }
  }

  public MinerMenu(int windowId, Inventory inv, MinerBlockEntity entity) {
    super(ModMenuTypes.MINER_MENU.get(), windowId);
    blockEntity = entity;

    addPlayerInventory(inv);
    addPlayerHotbar(inv);

    blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
      // Replace regular slot with mining pipe slot
      addSlot(new MiningPipeSlot(handler, 0, 8, 52));
    });
  }

  // Constructor for the client side
  public MinerMenu(int windowId, Inventory inv, FriendlyByteBuf data) {
    this(windowId, inv, getBlockEntity(inv, data));
  }

  private static MinerBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
    BlockEntity entity = inv.player.level.getBlockEntity(data.readBlockPos());
    return (MinerBlockEntity) entity;
  }

  @Override
  public boolean stillValid(@Nonnull Player player) {
    return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(),
        blockEntity.getBlockPos()), player, ModBlocks.MINER.get());
  }

  private void addPlayerInventory(Inventory playerInventory) {
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9,
            8 + col * 18, 84 + row * 18));
      }
    }
  }

  private void addPlayerHotbar(Inventory playerInventory) {
    for (int i = 0; i < 9; i++) {
      addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
    }
  }

  // Handle shift-clicking items
  @Override
  public ItemStack quickMoveStack(@Nonnull Player player, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot.hasItem()) {
      ItemStack stack = slot.getItem();
      itemstack = stack.copy();

      // If clicking in player inventory (0-35)
      if (index < 36) {
        // Try to move to pipe slot (36), but only if it's a valid pipe
        if (stack.is(MINING_PIPES)) {
          if (!this.moveItemStackTo(stack, 36, 37, false)) {
            return ItemStack.EMPTY;
          }
        } else {
          return ItemStack.EMPTY;
        }
      }
      // If clicking in pipe slot (36)
      else {
        // Try to move to player inventory (0-35)
        if (!this.moveItemStackTo(stack, 0, 36, false)) {
          return ItemStack.EMPTY;
        }
      }

      if (stack.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }
    return itemstack;
  }

  public MinerBlockEntity getBlockEntity() {
    return blockEntity;
  }

  public Map<ResourceLocation, Integer> getScannedBlocks() {
    return blockEntity.getScannedBlocks();
  }

  public void setScannedBlocks(Map<ResourceLocation, Integer> blocks) {
    this.scannedBlocks = blocks;
  }

  @Override
  public void broadcastChanges() {
    super.broadcastChanges();

    // Ensure the client slot data is synced with the server
    blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
      ItemStack stack = handler.getStackInSlot(0);
      this.slots.get(PIPE_SLOT_INDEX).set(stack);
    });
  }
}
