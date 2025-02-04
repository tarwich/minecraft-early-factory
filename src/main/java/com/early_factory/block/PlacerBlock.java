package com.early_factory.block;

import javax.annotation.Nonnull;

import com.early_factory.ModBlockEntities;
import com.early_factory.block.entity.PlacerBlockEntity;

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

public class PlacerBlock extends BaseEntityBlock {
  public static final DirectionProperty FACING = BlockStateProperties.FACING;

  public PlacerBlock() {
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
    if (state.getBlock() != newState.getBlock()) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof PlacerBlockEntity) {
        ((PlacerBlockEntity) blockEntity).getItemHandler().setStackInSlot(0, net.minecraft.world.item.ItemStack.EMPTY);
      }
    }
    super.onRemove(state, level, pos, newState, isMoving);
  }

  @Override
  public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
      @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
    if (!level.isClientSide()) {
      BlockEntity entity = level.getBlockEntity(pos);
      if (entity instanceof PlacerBlockEntity) {
        NetworkHooks.openScreen(((ServerPlayer) player), (PlacerBlockEntity) entity, pos);
        return InteractionResult.CONSUME;
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }

  @Override
  public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
    return new PlacerBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
      @Nonnull BlockEntityType<T> type) {
    return createTickerHelper(type, ModBlockEntities.PLACER.get(),
        (level1, pos, state1, be) -> ((PlacerBlockEntity) be).tick(level1, pos, state1));
  }
}
