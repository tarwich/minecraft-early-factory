package com.early_factory.block;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import com.early_factory.block.entity.MinerBlockEntity;

public class MinerBlock extends Block implements EntityBlock {
  public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.NORTH, Direction.SOUTH,
      Direction.EAST, Direction.WEST);

  public MinerBlock() {
    super(Properties.of(Material.METAL)
        .strength(4.0f)
        .requiresCorrectToolForDrops());
    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

  @Override
  public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos,
      @Nonnull CollisionContext context) {
    return SHAPE;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
  }

  @Override
  public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
      InteractionHand hand, BlockHitResult trace) {
    if (!level.isClientSide) {
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof MinerBlockEntity) {
        NetworkHooks.openScreen((ServerPlayer) player, (MinerBlockEntity) be, pos);
      }
    }
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new MinerBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
      BlockEntityType<T> type) {
    if (level.isClientSide()) {
      return null;
    }
    return (level1, pos, state1, be) -> {
      if (be instanceof MinerBlockEntity miner) {
        miner.tickServer();
      }
    };
  }
}
