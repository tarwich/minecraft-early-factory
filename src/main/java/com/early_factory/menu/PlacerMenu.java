package com.early_factory.menu;

import javax.annotation.Nonnull;

import com.early_factory.EarlyFactory;
import com.early_factory.block.entity.PlacerBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class PlacerMenu extends AbstractContainerMenu {
  private final PlacerBlockEntity blockEntity;
  private final Level level;
  private final ContainerLevelAccess containerLevelAccess;

  public PlacerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
    this(id, inv, inv.player.level.getBlockEntity(extraData.readBlockPos()));
  }

  public PlacerMenu(int id, Inventory inv, BlockEntity entity) {
    super(ModMenuTypes.PLACER.get(), id);
    checkContainerSize(inv, 1);
    blockEntity = (PlacerBlockEntity) entity;
    this.level = inv.player.level;
    this.containerLevelAccess = ContainerLevelAccess.create(level, blockEntity.getBlockPos());

    // Add block entity slot
    addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 80, 35));

    // Add inventory slots
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 9; col++) {
        addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
      }
    }

    // Add hotbar slots
    for (int col = 0; col < 9; col++) {
      addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }
  }

  @Override
  public boolean stillValid(@Nonnull Player player) {
    return stillValid(containerLevelAccess, player, EarlyFactory.PLACER.get());
  }

  @Override
  public ItemStack quickMoveStack(@Nonnull Player player, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);

    if (slot.hasItem()) {
      ItemStack itemstack1 = slot.getItem();
      itemstack = itemstack1.copy();

      if (index < 1) {
        if (!this.moveItemStackTo(itemstack1, 1, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
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
}
