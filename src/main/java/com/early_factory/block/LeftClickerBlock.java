package com.early_factory.block;

import javax.annotation.Nonnull;

import com.early_factory.ModBlockEntities;
import com.early_factory.block.entity.LeftClickerBlockEntity;

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

public class LeftClickerBlock extends DirectionalBlock implements EntityBlock {
  public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

  public LeftClickerBlock() {
    super(BlockBehaviour.Properties.of(Material.WOOD)
        .strength(3.5f)
        .requiresCorrectToolForDrops());

    // Set default facing direction
    this.registerDefaultState(this.stateDefinition.any()
        .setValue(FACING, Direction.NORTH));
  }

  @Override
  protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
    super.createBlockStateDefinition(builder);
    builder.add(FACING);
  }

  @Override
  public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
    // Get the opposite of the direction the player is looking at
    // This makes the block face away from the player
    return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
  }

  @Override
  public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
    return new LeftClickerBlockEntity(pos, state);
  }

  @Override
  public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
      @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
    if (!level.isClientSide()) {
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof LeftClickerBlockEntity) {
        NetworkHooks.openScreen((ServerPlayer) player, (LeftClickerBlockEntity) be, pos);
        return InteractionResult.sidedSuccess(true);
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide());
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state,
      @Nonnull BlockEntityType<T> blockEntityType) {
    return level.isClientSide ? null
        : createTickerHelper(blockEntityType, ModBlockEntities.LEFT_CLICKER.get(),
            (level1, pos, state1, blockEntity) -> {
              // Minecraft runs at 20 ticks per second, so waiting 10 ticks = 0.5 seconds
              if (level1.getGameTime() % 10 == 0) {
                ((LeftClickerBlockEntity) blockEntity).tick(level1, pos, state1);
              }
            });
  }

  private static <T extends BlockEntity> BlockEntityTicker<T> createTickerHelper(BlockEntityType<T> actualType,
      BlockEntityType<?> expectedType, BlockEntityTicker<? super BlockEntity> ticker) {
    return expectedType == actualType ? (BlockEntityTicker<T>) ticker : null;
  }

  @Override
  public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    if (!state.is(newState.getBlock())) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof LeftClickerBlockEntity leftClicker) {
        leftClicker.dropInventory(level, pos);
      }
    }
    super.onRemove(state, level, pos, newState, isMoving);
  }
}
