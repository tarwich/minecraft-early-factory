package com.early_factory.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;

public class BreakerBlock extends Block {
  public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

  public BreakerBlock() {
    super(BlockBehaviour.Properties.of(Material.METAL)
        .strength(3.5f)
        .requiresCorrectToolForDrops());

    // Set default facing direction
    this.registerDefaultState(this.stateDefinition.any()
        .setValue(FACING, Direction.NORTH));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }
}
