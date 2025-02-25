package com.early_factory.block;

import javax.annotation.Nonnull;

import com.early_factory.pipe.NetworkManager;
import com.early_factory.pipe.NetworkManagerProvider;
import com.early_factory.pipe.PipeNetwork;
import com.mojang.math.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
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

  private static final Vector3f INPUT_COLOR = new Vector3f(0.0F, 0.8F, 0.0F); // Green
  private static final Vector3f OUTPUT_COLOR = new Vector3f(0.8F, 0.0F, 0.0F); // Red

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

  @Override
  public void onPlace(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
      @Nonnull BlockState oldState, boolean isMoving) {
    if (!level.isClientSide && oldState.getBlock() != this) {
      // Get NetworkManager (you'll need to store/access this somewhere)
      NetworkManager networkManager = getNetworkManager(level);
      networkManager.onPipePlaced(pos);
    }
  }

  @Override
  public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos,
      @Nonnull BlockState newState, boolean isMoving) {
    if (!level.isClientSide && newState.getBlock() != this) {
      NetworkManager networkManager = getNetworkManager(level);
      networkManager.onPipeRemoved(pos);
    }
    super.onRemove(state, level, pos, newState, isMoving);
  }

  // You'll need to implement this method to access your NetworkManager instance
  private NetworkManager getNetworkManager(Level level) {
    return NetworkManagerProvider.get(level);
  }

  @Override
  public InteractionResult use(BlockState state, Level level, BlockPos pos,
      Player player, InteractionHand hand, BlockHitResult hit) {
    if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
      Direction face = hit.getDirection();
      BlockPos neighborPos = pos.relative(face);

      // Check if the clicked side has an inventory
      if (level.getBlockEntity(neighborPos) != null &&
          level.getBlockEntity(neighborPos).getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {

        // Toggle the inventory mode
        NetworkManager networkManager = getNetworkManager(level);
        PipeNetwork network = networkManager.findNetworkForPipe(pos);
        if (network != null) {
          String oldMode = network.getInventoryMode(pos, face);
          network.toggleInventoryMode(pos, face);
          String newMode = network.getInventoryMode(pos, face);

          // Send feedback to player
          player.displayClientMessage(
              Component.literal("Inventory mode: " + newMode), true);

          // Spawn particles at the connection point
          spawnModeParticles((ServerLevel) level, pos, face, newMode);

          return InteractionResult.SUCCESS;
        }
      }
    }
    return InteractionResult.PASS;
  }

  private void spawnModeParticles(ServerLevel level, BlockPos pos, Direction face, String mode) {
    // Calculate particle position at the connection point
    double x = pos.getX() + 0.5 + face.getStepX() * 0.5;
    double y = pos.getY() + 0.5 + face.getStepY() * 0.5;
    double z = pos.getZ() + 0.5 + face.getStepZ() * 0.5;

    Vector3f color = switch (mode) {
      case "INPUT" -> INPUT_COLOR;
      case "OUTPUT" -> OUTPUT_COLOR;
      default -> null;
    };

    if (color != null) {
      level.sendParticles(new DustParticleOptions(color, 1.0F),
          x, y, z, 8, // count
          0.1, 0.1, 0.1, // spread
          0.0); // speed
    }
  }
}
