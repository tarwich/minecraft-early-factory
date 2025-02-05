package com.early_factory.block.entity;

import java.util.List;

import com.early_factory.ModBlockEntities;
import com.early_factory.block.CollectorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
        pos.getX() - COLLECTION_RANGE, pos.getY() - 1, pos.getZ() - COLLECTION_RANGE,
        pos.getX() + (COLLECTION_RANGE + 1), pos.getY() + 2, pos.getZ() + (COLLECTION_RANGE + 1));

    // Get all items in range
    List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, collectionBox);

    // Process each item
    for (ItemEntity item : items) {
      // Skip if another collector is closer to this item
      AABB itemBox = item.getBoundingBox();
      if (CollectorBlock.shouldDeferToCloserCollector(level, pos, itemBox)) {
        continue;
      }

      // Skip if item should not be collected due to nearby player
      if (!blockEntity.shouldCollectItem(level, pos, item)) {
        continue;
      }

      // Calculate direction to collector
      double dx = pos.getX() + 0.5D - item.getX();
      double dy = pos.getY() + 0.5D - item.getY();
      double dz = pos.getZ() + 0.5D - item.getZ();

      double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

      // If item is close enough to collector, try to insert into adjacent inventory
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

      // If not inserted into inventory, keep pulling towards collector
      if (!item.isRemoved()) {
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
}
