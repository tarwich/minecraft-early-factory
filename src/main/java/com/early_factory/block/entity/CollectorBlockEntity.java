package com.early_factory.block.entity;

import java.util.List;

import com.early_factory.block.CollectorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public class CollectorBlockEntity extends BlockEntity {
  private static final int COLLECTION_RANGE = 5;

  public CollectorBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.COLLECTOR.get(), pos, state);
  }

  public static void tick(Level level, BlockPos pos, BlockState state, CollectorBlockEntity blockEntity) {
    if (level.isClientSide()) {
      return;
    }

    // Define collection area
    AABB collectionBox = new AABB(
        pos.getX() - 5, pos.getY() - 1, pos.getZ() - 5,
        pos.getX() + 6, pos.getY() + 2, pos.getZ() + 6);

    // Get all items in range
    List<ItemEntity> items = level.getEntitiesOfClass(
        ItemEntity.class,
        collectionBox);

    // Process each item
    for (ItemEntity item : items) {
      // Skip if another collector is closer to this item
      AABB itemBox = item.getBoundingBox();
      if (CollectorBlock.shouldDeferToCloserCollector(level, pos, itemBox)) {
        continue;
      }

      // Calculate direction to collector
      double dx = pos.getX() + 0.5D - item.getX();
      double dy = pos.getY() + 0.5D - item.getY();
      double dz = pos.getZ() + 0.5D - item.getZ();

      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

      // If item is close enough to collector, try to insert into adjacent inventory
      if (distance < 1.0D) {
        ItemStack stack = item.getItem();
        boolean inserted = false;

        // Try each direction
        for (Direction direction : Direction.values()) {
          if (inserted)
            break;

          BlockEntity targetEntity = level.getBlockEntity(pos.relative(direction));
          if (targetEntity != null) {
            // Try to get item handler capability
            IItemHandler inventory = targetEntity
                .getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                .resolve().orElse(null);

            if (inventory != null) {
              // Try to insert into each slot
              for (int i = 0; i < inventory.getSlots() && !stack.isEmpty(); i++) {
                stack = inventory.insertItem(i, stack, false);
              }

              // If we inserted all items, remove the entity
              if (stack.isEmpty()) {
                item.discard();
                inserted = true;
              }
            }
          }
        }
      }

      // If not inserted into inventory, keep pulling towards collector
      if (!item.isRemoved()) {
        double speed = 0.3D;
        item.setDeltaMovement(
            dx / distance * speed,
            dy / distance * speed,
            dz / distance * speed);
      }
    }

    // Get all experience orbs in range
    List<ExperienceOrb> xpOrbs = level.getEntitiesOfClass(
        ExperienceOrb.class,
        collectionBox);

    // Process each experience orb
    for (ExperienceOrb orb : xpOrbs) {
      // Skip if another collector is closer to this orb
      AABB orbBox = orb.getBoundingBox();
      if (CollectorBlock.shouldDeferToCloserCollector(level, pos, orbBox)) {
        continue;
      }

      // Existing XP collection logic...
      // ... rest of your XP collection code ...
    }
  }
}
