package com.early_factory.menu;

import com.early_factory.ModMenuTypes;
import com.early_factory.block.entity.CollectorBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class CollectorMenu extends AbstractContainerMenu {
  private final CollectorBlockEntity blockEntity;
  private final IItemHandler inventory;

  public CollectorMenu(int containerId, Inventory playerInventory, CollectorBlockEntity entity) {
    super(ModMenuTypes.COLLECTOR_MENU.get(), containerId);
    this.blockEntity = entity;
    this.inventory = entity.getInventory();

    // Add collector inventory slots (3 rows of 9)
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new SlotItemHandler(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
      }
    }

    // Add player inventory slots
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
      }
    }

    // Add player hotbar slots
    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }
  }

  public CollectorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
    this(containerId, playerInventory, getBlockEntity(playerInventory, extraData));
  }

  private static CollectorBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
    BlockEntity entity = playerInventory.player.level.getBlockEntity(extraData.readBlockPos());
    if (entity instanceof CollectorBlockEntity) {
      return (CollectorBlockEntity) entity;
    }
    throw new IllegalStateException("Block entity is not correct! " + entity);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);

    if (slot.hasItem()) {
      ItemStack itemstack1 = slot.getItem();
      itemstack = itemstack1.copy();

      if (index < 27) {
        if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
        return ItemStack.EMPTY;
      }

      if (itemstack1.isEmpty()) {
        slot.set(ItemStack.EMPTY);
      } else {
        slot.setChanged();
      }
    }

    return itemstack;
  }

  @Override
  public boolean stillValid(Player player) {
    return this.blockEntity.stillValid(player);
  }
}
