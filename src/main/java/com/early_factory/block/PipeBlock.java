package com.early_factory.block;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class PipeBlock extends Block {
  public static final BooleanProperty NORTH = BooleanProperty.create("north");
  public static final BooleanProperty SOUTH = BooleanProperty.create("south");
  public static final BooleanProperty EAST = BooleanProperty.create("east");
  public static final BooleanProperty WEST = BooleanProperty.create("west");
  public static final BooleanProperty UP = BooleanProperty.create("up");
  public static final BooleanProperty DOWN = BooleanProperty.create("down");

  // Create static VoxelShapes for each pipe segment
  private static final VoxelShape CENTER_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
  private static final VoxelShape NORTH_SHAPE = Block.box(5, 5, 0, 11, 11, 5);
  private static final VoxelShape SOUTH_SHAPE = Block.box(5, 5, 11, 11, 11, 16);
  private static final VoxelShape EAST_SHAPE = Block.box(11, 5, 5, 16, 11, 11);
  private static final VoxelShape WEST_SHAPE = Block.box(0, 5, 5, 5, 11, 11);
  private static final VoxelShape UP_SHAPE = Block.box(5, 11, 5, 11, 16, 11);
  private static final VoxelShape DOWN_SHAPE = Block.box(5, 0, 5, 11, 5, 11);

  public PipeBlock(Properties properties) {
    super(properties);
    registerDefaultState(this.stateDefinition.any()
        .setValue(NORTH, false)
        .setValue(SOUTH, false)
        .setValue(EAST, false)
        .setValue(WEST, false)
        .setValue(UP, false)
        .setValue(DOWN, false));
  }

  @Override
  protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
  }

  @Override
  @Nonnull
  public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
    return updateConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
  }

  @Override
  @Nonnull
  public BlockState updateShape(@Nonnull BlockState state, @Nonnull Direction direction,
      @Nonnull BlockState neighborState, @Nonnull LevelAccessor level,
      @Nonnull BlockPos currentPos, @Nonnull BlockPos neighborPos) {
    return updateConnections(level, currentPos, state);
  }

  @Nonnull
  private BlockState updateConnections(@Nonnull LevelAccessor level, @Nonnull BlockPos pos,
      @Nonnull BlockState state) {
    return state
        .setValue(NORTH, canConnect(level, pos, Direction.NORTH))
        .setValue(SOUTH, canConnect(level, pos, Direction.SOUTH))
        .setValue(EAST, canConnect(level, pos, Direction.EAST))
        .setValue(WEST, canConnect(level, pos, Direction.WEST))
        .setValue(UP, canConnect(level, pos, Direction.UP))
        .setValue(DOWN, canConnect(level, pos, Direction.DOWN));
  }

  private boolean canConnect(@Nonnull LevelAccessor level, @Nonnull BlockPos pos,
      @Nonnull Direction direction) {
    BlockPos neighborPos = pos.relative(direction);
    BlockState neighborState = level.getBlockState(neighborPos);

    // Check if neighbor is another pipe
    if (neighborState.getBlock() instanceof PipeBlock) {
      return true;
    }

    // Check if neighbor has an inventory
    BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
    return neighborEntity != null && neighborEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    VoxelShape shape = CENTER_SHAPE;

    if (state.getValue(NORTH))
      shape = Shapes.or(shape, NORTH_SHAPE);
    if (state.getValue(SOUTH))
      shape = Shapes.or(shape, SOUTH_SHAPE);
    if (state.getValue(EAST))
      shape = Shapes.or(shape, EAST_SHAPE);
    if (state.getValue(WEST))
      shape = Shapes.or(shape, WEST_SHAPE);
    if (state.getValue(UP))
      shape = Shapes.or(shape, UP_SHAPE);
    if (state.getValue(DOWN))
      shape = Shapes.or(shape, DOWN_SHAPE);

    return shape;
  }
}
