package com.early_factory.block.entity;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.early_factory.ModBlockEntities;
import com.early_factory.block.CollectorBlock;
import com.early_factory.menu.CollectorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class CollectorBlockEntity extends BlockEntity implements MenuProvider {
  private static final int COLLECTION_RANGE = 5;
  private static final int INVENTORY_SIZE = 27; // Same size as a chest

  private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
    @Override
    protected void onContentsChanged(int slot) {
      setChanged();
    }
  };

  private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> inventory);

  public CollectorBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.COLLECTOR.get(), pos, state);
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    inventory.deserializeNBT(tag.getCompound("Inventory"));
  }

  @Override
  protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    tag.put("Inventory", inventory.serializeNBT());
  }

  public IItemHandler getInventory() {
    return inventory;
  }

  @Override
  public @Nonnull <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
    if (cap == ForgeCapabilities.ITEM_HANDLER) {
      return handler.cast();
    }
    return super.getCapability(cap, side);
  }

  @Override
  public void invalidateCaps() {
    super.invalidateCaps();
    handler.invalidate();
  }

  public static void tick(Level level, BlockPos pos, BlockState state, CollectorBlockEntity blockEntity) {
    if (level.isClientSide()) {
      return;
    }

    // First, try to output items to adjacent inventories
    blockEntity.outputItems(level, pos);

    // Then collect new items if there's space
    blockEntity.collectItems(level, pos);
  }

  private void outputItems(Level level, BlockPos pos) {
    // Try each direction
    for (Direction direction : Direction.values()) {
      BlockEntity targetEntity = level.getBlockEntity(pos.relative(direction));
      if (targetEntity == null)
        continue;

      IItemHandler targetInventory = targetEntity
          .getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
          .resolve().orElse(null);

      if (targetInventory == null)
        continue;

      // Try to move each item from our inventory to the target
      for (int fromSlot = 0; fromSlot < inventory.getSlots(); fromSlot++) {
        ItemStack stack = inventory.extractItem(fromSlot, Integer.MAX_VALUE, true);
        if (stack.isEmpty())
          continue;

        // Try to insert into target inventory
        ItemStack remaining = stack;
        for (int toSlot = 0; toSlot < targetInventory.getSlots() && !remaining.isEmpty(); toSlot++) {
          remaining = targetInventory.insertItem(toSlot, remaining, false);
        }

        // If we managed to insert any items, extract them from our inventory
        int inserted = stack.getCount() - remaining.getCount();
        if (inserted > 0) {
          inventory.extractItem(fromSlot, inserted, false);
        }
      }
    }
  }

  private void collectItems(Level level, BlockPos pos) {
    // Define collection area
    AABB collectionBox = new AABB(
        pos.getX() - COLLECTION_RANGE, pos.getY() - 1, pos.getZ() - COLLECTION_RANGE,
        pos.getX() + (COLLECTION_RANGE + 1), pos.getY() + 2, pos.getZ() + (COLLECTION_RANGE + 1));

    // Get all items in range
    List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, collectionBox);

    // Process each item
    for (ItemEntity item : items) {
      // Skip if another collector is closer to this item
      if (CollectorBlock.shouldDeferToCloserCollector(level, pos, item.getBoundingBox())) {
        continue;
      }

      // Skip if item should not be collected due to nearby player
      if (!shouldCollectItem(level, pos, item)) {
        continue;
      }

      // Try to insert the item into our inventory
      ItemStack remainingStack = ItemStack.EMPTY;
      ItemStack stackToInsert = item.getItem().copy();

      for (int slot = 0; slot < inventory.getSlots() && !stackToInsert.isEmpty(); slot++) {
        stackToInsert = inventory.insertItem(slot, stackToInsert, false);
      }

      // If we inserted any items, update or remove the entity
      if (stackToInsert.getCount() < item.getItem().getCount()) {
        if (stackToInsert.isEmpty()) {
          item.discard();
        } else {
          item.setItem(stackToInsert);
        }
      }

      // If we couldn't insert the item, pull it towards the collector
      if (!item.isRemoved()) {
        double dx = pos.getX() + 0.5D - item.getX();
        double dy = pos.getY() + 0.5D - item.getY();
        double dz = pos.getZ() + 0.5D - item.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double speed = 0.3D;

        item.setDeltaMovement(
            dx / distance * speed,
            dy / distance * speed,
            dz / distance * speed);
      }
    }
  }

  private boolean shouldCollectItem(Level level, BlockPos pos, ItemEntity itemEntity) {
    // Check for sneaking players within 2 blocks of the item
    List<Player> nearbyPlayers = level.getEntitiesOfClass(
        Player.class,
        new AABB(
            itemEntity.getX() - 2,
            itemEntity.getY() - 2,
            itemEntity.getZ() - 2,
            itemEntity.getX() + 2,
            itemEntity.getY() + 2,
            itemEntity.getZ() + 2),
        player -> player.isCrouching()); // Only check for sneaking players

    // If there are nearby sneaking players, check if they have a clear path to the
    // item
    for (Player player : nearbyPlayers) {
      // Create a ray from player eyes to item center
      Vec3 playerEyes = player.getEyePosition();
      Vec3 itemPos = itemEntity.position();

      // Check for blocks between player and item
      BlockHitResult hitResult = level.clip(new ClipContext(
          playerEyes,
          itemPos,
          ClipContext.Block.COLLIDER,
          ClipContext.Fluid.NONE,
          player));

      // If the ray doesn't hit any blocks, don't collect
      if (hitResult.getType() == HitResult.Type.MISS) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.early_factory.collector");
  }

  @Override
  public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
    return new CollectorMenu(containerId, inventory, this);
  }

  public boolean stillValid(Player player) {
    if (this.level.getBlockEntity(this.worldPosition) != this) {
      return false;
    }
    return player.distanceToSqr(this.worldPosition.getX() + 0.5D,
        this.worldPosition.getY() + 0.5D,
        this.worldPosition.getZ() + 0.5D) <= 64.0D;
  }
}
