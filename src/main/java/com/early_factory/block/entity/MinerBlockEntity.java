package com.early_factory.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
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
import com.early_factory.util.MiningTiers;

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
  private int currentYLevel;
  private double yLevelProgress;
  private ItemStack previousStack = ItemStack.EMPTY;
  private static final double MOVING_SPEED = 0.1D;
  private static final int DEPTH_PER_PIPE = 10;
  private static final int SCAN_RADIUS = 8;
  private static final int SCAN_DEPTH = 16;
  private static final boolean WEIGHTED_MINING = false;

  // Replace the scannedBlocks Set with a Map to track quantities
  private final java.util.Map<ResourceLocation, Integer> scannedBlocks = new java.util.HashMap<>();

  private static final int MINING_SPEED = 20; // 1 seconds (20 ticks per second)
  private int miningProgress = 0;

  public MinerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.MINER.get(), pos, state);
    resetDepth();
  }

  private void resetDepth() {
    this.currentYLevel = this.getBlockPos().getY();
  }

  private int getTargetYLevel() {
    ItemStack stack = itemHandler.getStackInSlot(0);
    int pipeCount = stack.getCount();
    return this.getBlockPos().getY() - (pipeCount * DEPTH_PER_PIPE);
  }

  public void tickServer() {
    int targetYLevel = getTargetYLevel();
    boolean isAdjustingLevel = this.currentYLevel != targetYLevel;

    if (isAdjustingLevel) {
      LOGGER.debug("Adjusting Y-level from {} to {}", this.currentYLevel, targetYLevel);
      int oldYLevel = this.currentYLevel;

      if (this.currentYLevel > targetYLevel) {
        yLevelProgress -= MOVING_SPEED;
        if (yLevelProgress <= -1.0) {
          this.currentYLevel--;
          yLevelProgress += 1.0;
        }
      } else if (this.currentYLevel < this.getBlockPos().getY()) {
        yLevelProgress += MOVING_SPEED;
        if (yLevelProgress >= 1.0) {
          this.currentYLevel++;
          yLevelProgress -= 1.0;
        }
      }

      // Scan whenever the y-level changes
      if (oldYLevel != this.currentYLevel) {
        scanBlocks();
      }

      setChanged();
      if (level != null && !level.isClientSide()) {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
      }
      return;
    }

    // Remove the old depth-based scan check since we're now scanning during
    // movement
    // Only do mining logic when we're not moving
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

    LOGGER.debug("Starting scan at Y-level: {}", currentYLevel);
    scannedBlocks.clear();
    BlockPos minerPos = this.getBlockPos();
    int currentY = this.currentYLevel;

    int blocksFound = 0; // For debug
    for (int y = currentY; y > currentY - SCAN_DEPTH && y > level.getMinBuildHeight(); y--) {
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
    return MiningTiers.isValidBlock(state);
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
    tag.putInt("currentYLevel", currentYLevel);

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
    currentYLevel = tag.getInt("currentYLevel");

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

  public int getCurrentYLevel() {
    return currentYLevel;
  }

  @Override
  public CompoundTag getUpdateTag() {
    CompoundTag tag = super.getUpdateTag();
    tag.put("inventory", itemHandler.serializeNBT());
    tag.putInt("currentYLevel", currentYLevel);

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

    if (tag.contains("inventory")) {
      itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    if (tag.contains("currentYLevel")) {
      currentYLevel = tag.getInt("currentYLevel");
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

    ItemStack toolStack = itemHandler.getStackInSlot(0);
    if (toolStack.isEmpty())
      return;

    ResourceLocation selectedBlock = null;

    if (WEIGHTED_MINING) {
      // Calculate total weight for available blocks we can mine
      int totalWeight = 0;
      Map<ResourceLocation, Integer> availableBlocks = new java.util.HashMap<>();

      for (Map.Entry<ResourceLocation, Integer> entry : scannedBlocks.entrySet()) {
        Block block = net.minecraft.core.Registry.BLOCK.get(entry.getKey());
        if (MiningTiers.canMineBlock(block, toolStack)) {
          availableBlocks.put(entry.getKey(), entry.getValue());
          totalWeight += entry.getValue();
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
          .filter(blockId -> MiningTiers.canMineBlock(
              net.minecraft.core.Registry.BLOCK.get(blockId),
              toolStack))
          .collect(java.util.stream.Collectors.toList());

      if (!mineableBlocks.isEmpty()) {
        // Pick a random block from the filtered list
        selectedBlock = mineableBlocks.get(level.getRandom().nextInt(mineableBlocks.size()));
      }
    }

    if (selectedBlock == null)
      return;

    // Create the block item
    Block block = net.minecraft.core.Registry.BLOCK.get(selectedBlock);
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
