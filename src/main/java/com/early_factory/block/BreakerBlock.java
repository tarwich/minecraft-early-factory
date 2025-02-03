package com.early_factory.block;

import com.early_factory.block.entity.BreakerBlockEntity;
import com.early_factory.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class BreakerBlock extends DirectionalBlock implements EntityBlock {
  public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

  public BreakerBlock() {
    super(BlockBehaviour.Properties.of(Material.WOOD)
        .strength(3.5f)
        .requiresCorrectToolForDrops());

    // Set default facing direction
    this.registerDefaultState(this.stateDefinition.any()
        .setValue(FACING, Direction.NORTH));
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(FACING);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    // Get the opposite of the direction the player is looking at
    // This makes the block face away from the player
    return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new BreakerBlockEntity(pos, state);
  }

  @Override
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
      InteractionHand hand, BlockHitResult hit) {
    if (!level.isClientSide()) {
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof BreakerBlockEntity) {
        NetworkHooks.openScreen((ServerPlayer) player, (BreakerBlockEntity) be, pos);
        return InteractionResult.sidedSuccess(true);
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
      BlockEntityType<T> blockEntityType) {
    return level.isClientSide ? null
        : createTickerHelper(blockEntityType, ModBlockEntities.BREAKER.get(),
            (level1, pos, state1, blockEntity) -> ((BreakerBlockEntity) blockEntity).tick(level1, pos, state1));
  }

  private static <T extends BlockEntity> BlockEntityTicker<T> createTickerHelper(BlockEntityType<T> actualType,
      BlockEntityType<?> expectedType, BlockEntityTicker<? super BlockEntity> ticker) {
    return expectedType == actualType ? (BlockEntityTicker<T>) ticker : null;
  }
}
