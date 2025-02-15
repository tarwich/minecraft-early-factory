package com.early_factory.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.early_factory.ModBlockEntities;
import com.early_factory.menu.MinerMenu;
import com.early_factory.block.entity.MinerBlockEntity;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

public class MinerBlockEntity extends BlockEntity implements MenuProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger("early_factory");

  private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
    @Override
    protected void onContentsChanged(int slot) {
      if (slot == 0) {
        ItemStack newStack = getStackInSlot(slot);
        if (!newStack.isEmpty() && !previousStack.isEmpty() &&
            !newStack.getItem().equals(previousStack.getItem())) {
          resetDepth();
        }
        previousStack = newStack.copy();
      }
      setChanged();
      if (level != null && !level.isClientSide()) {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
      }
    }
  };

  private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
  private double depth;
  private ItemStack previousStack = ItemStack.EMPTY;
  private static final double MOVING_SPEED = 0.25D;
  private static final int DEPTH_PER_PIPE = 10;
  private static final int SCAN_RADIUS = 8; // For 16x16 area (8 blocks in each direction)
  private static final int SCAN_DEPTH = 16;
  private static final boolean WEIGHTED_MINING = false;

  // Replace the scannedBlocks Set with a Map to track quantities
  private final java.util.Map<ResourceLocation, Integer> scannedBlocks = new java.util.HashMap<>();

  private static final int MINING_SPEED = 20; // 1 seconds (20 ticks per second)
  private int miningProgress = 0;
  private static final Map<ResourceLocation, Integer> TOOL_MINING_LEVELS = new java.util.HashMap<>();

  static {
    TOOL_MINING_LEVELS.put(new ResourceLocation("early_factory", "wooden_mining_pipe"), 0); // Wood level
    TOOL_MINING_LEVELS.put(new ResourceLocation("early_factory", "stone_mining_pipe"), 1); // Stone level
    TOOL_MINING_LEVELS.put(new ResourceLocation("early_factory", "iron_mining_pipe"), 2); // Iron level
    TOOL_MINING_LEVELS.put(new ResourceLocation("early_factory", "diamond_mining_pipe"), 3); // Diamond level
  }

  private static final Map<ResourceLocation, Integer> BLOCK_MINING_LEVELS = new java.util.HashMap<>();

  static {
    // Level 0 (Wood)
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "dirt"), 0);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "grass_block"), 0);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "sand"), 0);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "gravel"), 0);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "stone"), 0);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "coal_ore"), 0);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "redstone_ore"), 3);

    // Level 1 (Stone)
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "iron_ore"), 2);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "copper_ore"), 2);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "lapis_ore"), 2);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "gold_ore"), 2);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "redstone_ore"), 2);
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "emerald_ore"), 2);

    // Level 2 (Iron)
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "diamond_ore"), 3);

    // Level 3 (Diamond)
    BLOCK_MINING_LEVELS.put(new ResourceLocation("minecraft", "obsidian"), 3);
  }

  private double lastScannedDepth;

  public MinerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.MINER.get(), pos, state);
    resetDepth();
    lastScannedDepth = depth;
  }

  private void resetDepth() {
    this.depth = this.getBlockPos().getY();
  }

  private double getPossibleDepth() {
    ItemStack stack = itemHandler.getStackInSlot(0);
    int pipeCount = stack.getCount();
    return this.getBlockPos().getY() - (pipeCount * DEPTH_PER_PIPE);
  }

  public void tickServer() {
    double possibleDepth = getPossibleDepth();
    boolean isAdjustingDepth = Math.abs(this.depth - possibleDepth) > 0.001;

    if (isAdjustingDepth) {
      LOGGER.debug("Adjusting depth from {} to {}", this.depth, possibleDepth);
      if (this.depth > possibleDepth) {
        if (this.depth - possibleDepth <= MOVING_SPEED) {
          this.depth = possibleDepth;
        } else {
          this.depth = this.depth - MOVING_SPEED;
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
          level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
      } else if (this.depth < this.getBlockPos().getY()) {
        if (possibleDepth - this.depth <= MOVING_SPEED) {
          this.depth = possibleDepth;
        } else {
          this.depth = this.depth + MOVING_SPEED;
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
          level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
      }
      return;
    }

    // Only scan if depth has changed
    if (Math.abs(this.depth - lastScannedDepth) > 0.001) {
      scanBlocks();
      lastScannedDepth = this.depth;
    }

    // Mining logic
    if (!scannedBlocks.isEmpty()) {
      miningProgress++;
      if (miningProgress >= MINING_SPEED) {
        LOGGER.debug("Attempting to mine. Progress: {}/{}", miningProgress, MINING_SPEED);
        miningProgress = 0;
        tryMineBlock();
      }
    }
  }

  private void scanBlocks() {
    if (level == null)
      return;

    LOGGER.debug("Starting scan at depth: {}", depth);
    scannedBlocks.clear();
    BlockPos minerPos = this.getBlockPos();
    int currentDepth = (int) this.depth;

    int blocksFound = 0; // For debug
    for (int y = currentDepth; y > currentDepth - SCAN_DEPTH && y > level.getMinBuildHeight(); y--) {
      for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
        for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
          BlockPos pos = new BlockPos(minerPos.getX() + x, y, minerPos.getZ() + z);
          BlockState state = level.getBlockState(pos);

          if (isValidBlock(state)) {
            ResourceLocation blockId = net.minecraft.core.Registry.BLOCK.getKey(state.getBlock());
            scannedBlocks.merge(blockId, 1, Integer::sum);
            blocksFound++;
          }
        }
      }
    }
    LOGGER.debug("Scan complete. Found {} valid blocks. Types: {}", blocksFound, scannedBlocks.keySet());
    setChanged();
  }

  private boolean isValidBlock(BlockState state) {
    if (state.getFluidState().isEmpty()) {
      net.minecraft.world.level.block.Block block = state.getBlock();

      // Check if the block is a valid mineable type
      return block instanceof net.minecraft.world.level.block.GravelBlock ||
          block instanceof net.minecraft.world.level.block.SandBlock ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.BASE_STONE_NETHER) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.DIRT) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.COAL_ORES) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.IRON_ORES) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.GOLD_ORES) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.DIAMOND_ORES) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.REDSTONE_ORES) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.LAPIS_ORES) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.EMERALD_ORES) ||
          block.defaultBlockState().is(net.minecraft.tags.BlockTags.COPPER_ORES) ||
          block instanceof net.minecraft.world.level.block.GrassBlock;
    }
    return false;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.early_factory.miner");
  }

  @Nullable
  @Override
  public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
    return new MinerMenu(windowId, inventory, this);
  }

  @Override
  public void onLoad() {
    super.onLoad();
    lazyItemHandler = LazyOptional.of(() -> itemHandler);
  }

  @Override
  public void invalidateCaps() {
    super.invalidateCaps();
    lazyItemHandler.invalidate();
  }

  @Override
  protected void saveAdditional(CompoundTag tag) {
    tag.put("inventory", itemHandler.serializeNBT());
    tag.putDouble("depth", depth);
    tag.putDouble("lastScannedDepth", lastScannedDepth);

    // Save scanned blocks
    CompoundTag blocksTag = new CompoundTag();
    for (Map.Entry<ResourceLocation, Integer> entry : scannedBlocks.entrySet()) {
      blocksTag.putInt(entry.getKey().toString(), entry.getValue());
    }
    tag.put("scannedBlocks", blocksTag);

    super.saveAdditional(tag);
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    itemHandler.deserializeNBT(tag.getCompound("inventory"));
    depth = tag.getDouble("depth");
    lastScannedDepth = tag.getDouble("lastScannedDepth");

    // Load scanned blocks
    scannedBlocks.clear();
    CompoundTag blocksTag = tag.getCompound("scannedBlocks");
    for (String key : blocksTag.getAllKeys()) {
      scannedBlocks.put(new ResourceLocation(key), blocksTag.getInt(key));
    }
  }

  @Nonnull
  @Override
  public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
    if (cap == ForgeCapabilities.ITEM_HANDLER) {
      return lazyItemHandler.cast();
    }
    return super.getCapability(cap, side);
  }

  @Override
  public void setChanged() {
    if (level != null && !level.isClientSide()) {
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    super.setChanged();
  }

  public double getDepth() {
    return depth;
  }

  @Override
  public CompoundTag getUpdateTag() {
    CompoundTag tag = super.getUpdateTag();
    tag.put("inventory", itemHandler.serializeNBT());
    tag.putDouble("depth", depth);

    // Make sure we're including the scanned blocks data
    CompoundTag blocksTag = new CompoundTag();
    for (Map.Entry<ResourceLocation, Integer> entry : scannedBlocks.entrySet()) {
      blocksTag.putInt(entry.getKey().toString(), entry.getValue());
    }
    tag.put("scannedBlocks", blocksTag);

    return tag;
  }

  @Override
  public void handleUpdateTag(CompoundTag tag) {
    super.handleUpdateTag(tag);

    // Add this line to handle inventory updates
    if (tag.contains("inventory")) {
      itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    // Add this line to handle depth updates
    if (tag.contains("depth")) {
      depth = tag.getDouble("depth");
    }

    // Clear and reload the scanned blocks
    scannedBlocks.clear();
    CompoundTag blocksTag = tag.getCompound("scannedBlocks");
    for (String key : blocksTag.getAllKeys()) {
      scannedBlocks.put(new ResourceLocation(key), blocksTag.getInt(key));
    }
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
    CompoundTag tag = pkt.getTag();
    handleUpdateTag(tag);
  }

  // Update getter to return block counts instead of positions
  public Map<ResourceLocation, Integer> getScannedBlocks() {
    return java.util.Collections.unmodifiableMap(scannedBlocks);
  }

  private void tryMineBlock() {
    if (level == null || scannedBlocks.isEmpty())
      return;

    // Get the current tool's mining level
    ItemStack toolStack = itemHandler.getStackInSlot(0);
    if (toolStack.isEmpty())
      return;

    ResourceLocation toolType = net.minecraft.core.Registry.ITEM.getKey(toolStack.getItem());
    int toolLevel = TOOL_MINING_LEVELS.getOrDefault(toolType, 0);

    ResourceLocation selectedBlock = null;

    if (WEIGHTED_MINING) {
      // Calculate total weight for available blocks we can mine
      int totalWeight = 0;
      Map<ResourceLocation, Integer> availableBlocks = new java.util.HashMap<>();

      for (Map.Entry<ResourceLocation, Integer> entry : scannedBlocks.entrySet()) {
        int blockLevel = BLOCK_MINING_LEVELS.getOrDefault(entry.getKey(), 1);

        int weight = entry.getValue();
        if (blockLevel <= toolLevel) {
          availableBlocks.put(entry.getKey(), weight);
          totalWeight += weight;
        }
      }

      if (totalWeight == 0)
        return;

      // Random selection weighted by quantity
      int random = level.getRandom().nextInt(totalWeight);
      for (Map.Entry<ResourceLocation, Integer> entry : availableBlocks.entrySet()) {
        random -= entry.getValue();
        if (random < 0) {
          selectedBlock = entry.getKey();
          break;
        }
      }
    } else {
      // Get list of blocks we can mine with current tool
      List<ResourceLocation> mineableBlocks = scannedBlocks.keySet().stream()
          .filter(block -> BLOCK_MINING_LEVELS.getOrDefault(block, 1) <= toolLevel)
          .collect(java.util.stream.Collectors.toList());

      if (!mineableBlocks.isEmpty()) {
        // Pick a random block from the filtered list
        selectedBlock = mineableBlocks.get(level.getRandom().nextInt(mineableBlocks.size()));
      }
    }

    if (selectedBlock == null)
      return;

    // Create the block item
    net.minecraft.world.level.block.Block block = net.minecraft.core.Registry.BLOCK.get(selectedBlock);
    ItemStack minedStack = new ItemStack(block.asItem());

    // Try to insert into adjacent inventory
    for (Direction direction : Direction.values()) {
      BlockPos targetPos = worldPosition.relative(direction);
      BlockEntity targetEntity = level.getBlockEntity(targetPos);

      if (targetEntity != null) {
        LazyOptional<IItemHandler> capability = targetEntity.getCapability(ForgeCapabilities.ITEM_HANDLER,
            direction.getOpposite());

        if (capability.isPresent()) {
          AtomicBoolean inserted = new AtomicBoolean(false);
          capability.ifPresent(handler -> {
            ItemStack remaining = minedStack.copy(); // Make a copy to work with
            for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
              remaining = handler.insertItem(i, remaining, false);
            }
            inserted.set(remaining.isEmpty());
          });

          if (inserted.get()) {
            // Debug output
            if (level.isClientSide()) {
              LOGGER.debug("Successfully inserted {} into inventory at {}", minedStack, targetPos);
            }
            return; // Exit after successful insertion
          }
        }
      }
    }

    // Debug output if no insertion happened
    if (level.isClientSide()) {
      LOGGER.debug("Failed to insert {}. No valid inventory found.", minedStack);
    }
  }
}
