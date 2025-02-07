package com.early_factory.block;

import java.util.List;

import javax.annotation.Nonnull;

import com.early_factory.ModBlockEntities;
import com.early_factory.block.entity.CollectorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class CollectorBlock extends BaseEntityBlock {
  public CollectorBlock() {
    super(Properties.of(Material.WOOD)
        .strength(2.5F)
        .noOcclusion());
  }

  @Override
  public RenderShape getRenderShape(@Nonnull BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
    return new CollectorBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
      @Nonnull BlockEntityType<T> type) {
    return createTickerHelper(type, ModBlockEntities.COLLECTOR.get(),
        CollectorBlockEntity::tick);
  }

  /**
   * Checks if there is a closer collector to the given entity box
   *
   * @return true if this collector should defer to another collector
   */
  public static boolean shouldDeferToCloserCollector(Level level, BlockPos thisPos, AABB entityBox) {
    // Get center point of the entity box for distance calculations
    double entityX = entityBox.getCenter().x;
    double entityY = entityBox.getCenter().y;
    double entityZ = entityBox.getCenter().z;

    // Find all collector blocks within range
    int searchRadius = 10; // Adjust based on collector range
    List<BlockPos> collectors = BlockPos.betweenClosedStream(
        thisPos.offset(-searchRadius, -searchRadius, -searchRadius),
        thisPos.offset(searchRadius, searchRadius, searchRadius))
        .filter(pos -> level.getBlockState(pos).getBlock() instanceof CollectorBlock)
        .filter(pos -> !pos.equals(thisPos)) // Exclude this collector
        .map(BlockPos::immutable)
        .toList();

    if (collectors.isEmpty()) {
      return false;
    }

    // Calculate this collector's distance to the entity
    double thisDistance = thisPos.distToCenterSqr(entityX, entityY, entityZ);

    // Check if any other collector is closer
    return collectors.stream().anyMatch(otherPos -> otherPos.distToCenterSqr(entityX, entityY, entityZ) < thisDistance);
  }

  @Override
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
      InteractionHand hand, BlockHitResult hit) {
    if (!level.isClientSide()) {
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof CollectorBlockEntity) {
        NetworkHooks.openScreen((ServerPlayer) player, (CollectorBlockEntity) be, pos);
      }
      return InteractionResult.CONSUME;
    }
    return InteractionResult.SUCCESS;
  }
}
