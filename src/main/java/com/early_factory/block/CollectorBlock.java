package com.early_factory.block;

import com.early_factory.block.entity.CollectorBlockEntity;
import com.early_factory.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class CollectorBlock extends BaseEntityBlock {
  public CollectorBlock() {
    super(Properties.of(Material.WOOD)
        .strength(2.5F)
        .noOcclusion());
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new CollectorBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
      BlockEntityType<T> type) {
    return createTickerHelper(type, ModBlockEntities.COLLECTOR.get(),
        CollectorBlockEntity::tick);
  }
}
