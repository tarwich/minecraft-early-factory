package com.early_factory.block;

import javax.annotation.Nonnull;

import com.early_factory.ModBlockEntities;
import com.early_factory.block.entity.RightClickerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class RightClickerBlock extends BaseEntityBlock {
  public static final DirectionProperty FACING = BlockStateProperties.FACING;

  public RightClickerBlock() {
    super(BlockBehaviour.Properties.of(Material.WOOD)
        .strength(3.5f)
        .requiresCorrectToolForDrops());
    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  @Override
  protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
    return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
  }

  @Override
  public RenderShape getRenderShape(@Nonnull BlockState state) {
    return RenderShape.MODEL;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
      @Nonnull BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock())) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof RightClickerBlockEntity rightClicker) {
        rightClicker.dropInventory(level, pos);
      }
    }
    super.onRemove(state, level, pos, newState, isMoving);
  }

  @Override
  public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
      @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
    if (!level.isClientSide()) {
      BlockEntity entity = level.getBlockEntity(pos);
      if (entity instanceof RightClickerBlockEntity) {
        NetworkHooks.openScreen(((ServerPlayer) player), (RightClickerBlockEntity) entity, pos);
        return InteractionResult.CONSUME;
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }

  @Override
  public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
    return new RightClickerBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
      @Nonnull BlockEntityType<T> type) {
    return createTickerHelper(type, ModBlockEntities.RIGHT_CLICKER.get(),
        (level1, pos, state1, be) -> ((RightClickerBlockEntity) be).tick(level1, pos, state1));
  }
}
