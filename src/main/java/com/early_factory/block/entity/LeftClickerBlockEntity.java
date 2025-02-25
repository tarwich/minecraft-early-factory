package com.early_factory.block.entity;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.early_factory.ModBlockEntities;
import com.early_factory.block.LeftClickerBlock;
import com.early_factory.menu.LeftClickerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

public class LeftClickerBlockEntity extends BlockEntity implements MenuProvider {
  private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
    @Override
    protected void onContentsChanged(int slot) {
      setChanged();
    }
  };

  private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

  private FakePlayer fakePlayer;

  public LeftClickerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.LEFT_CLICKER.get(), pos, state);
  }

  @Override
  public void load(@Nonnull CompoundTag tag) {
    super.load(tag);
    itemHandler.deserializeNBT(tag.getCompound("inventory"));
  }

  @Override
  protected void saveAdditional(@Nonnull CompoundTag tag) {
    tag.put("inventory", itemHandler.serializeNBT());
    super.saveAdditional(tag);
  }

  @Override
  public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
    if (cap == ForgeCapabilities.ITEM_HANDLER) {
      return lazyItemHandler.cast();
    }
    return super.getCapability(cap, side);
  }

  @Override
  public void invalidateCaps() {
    super.invalidateCaps();
    lazyItemHandler.invalidate();
  }

  public ItemStackHandler getItemHandler() {
    return itemHandler;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.early_factory.left_clicker");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
    return new LeftClickerMenu(id, inventory, this);
  }

  private void breakBlock(Level level, BlockPos pos, FakePlayer fakePlayer, ItemStack tool, boolean shouldDamageTool) {
    // Break the block
    level.destroyBlock(pos, true, fakePlayer);

    // Damage the tool if requested
    if (shouldDamageTool) {
      tool.hurtAndBreak(1, fakePlayer, (player) -> {
      });
    }
  }

  private void breakTreeRecursively(Level level, BlockPos startPos, FakePlayer fakePlayer, ItemStack tool,
      Set<BlockPos> visited) {
    if (visited.size() > 1000 || !visited.add(startPos)) { // Safety limit and cycle detection
      return;
    }

    BlockState state = level.getBlockState(startPos);
    boolean isLog = state.is(net.minecraft.tags.BlockTags.LOGS);
    boolean isLeaves = state.is(net.minecraft.tags.BlockTags.LEAVES);

    // Only continue if it's part of a tree
    if (!isLog && !isLeaves) {
      return;
    }

    // Break block without damaging tool during recursive calls
    breakBlock(level, startPos, fakePlayer, tool, false);

    // For logs, check all directions. For leaves, only check adjacent blocks
    int range = isLog ? 1 : 1;

    // Check surrounding blocks
    for (int x = -range; x <= range; x++) {
      for (int y = -range; y <= range; y++) {
        for (int z = -range; z <= range; z++) {
          if (x == 0 && y == 0 && z == 0)
            continue;

          BlockPos newPos = startPos.offset(x, y, z);
          BlockState newState = level.getBlockState(newPos);

          if (newState.is(net.minecraft.tags.BlockTags.LOGS) || newState.is(net.minecraft.tags.BlockTags.LEAVES)) {
            breakTreeRecursively(level, newPos, fakePlayer, tool, visited);
          }
        }
      }
    }
  }

  public void tick(Level level, BlockPos pos, BlockState state) {
    if (level.isClientSide()) {
      return;
    }

    // Initialize fakePlayer if needed
    if (fakePlayer == null) {
      fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) level);
    }

    Direction facing = state.getValue(LeftClickerBlock.FACING);
    BlockPos targetPos = pos.relative(facing);
    BlockState targetState = level.getBlockState(targetPos);

    // Skip if target is air, a growing plant, or any fluid
    if (targetState.isAir() || isGrowingPlant(targetState) || !targetState.getFluidState().isEmpty()) {
      return;
    }

    ItemStack tool = itemHandler.getStackInSlot(0);
    if (!tool.isEmpty()) {
      // Use our cached fake player
      fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());

      // Position the fake player behind the left clicker block
      BlockPos playerPos = pos.relative(facing.getOpposite());
      fakePlayer.setPos(playerPos.getX() + 0.5, playerPos.getY() + 0.5, playerPos.getZ() + 0.5);

      // Try to break the block if it's a tool
      if (tool.getItem() instanceof DiggerItem) {
        // Start breaking the block
        targetState.attack(level, targetPos, fakePlayer);

        // If it's a log, break the whole tree
        if (targetState.is(net.minecraft.tags.BlockTags.LOGS)) {
          breakTreeRecursively(level, targetPos, fakePlayer, tool, new HashSet<>());
          // Damage tool once after breaking the whole tree
          tool.hurtAndBreak(1, fakePlayer, (player) -> {
          });
        } else {
          // Just break the single block with tool damage
          breakBlock(level, targetPos, fakePlayer, tool, true);
        }

        // Update the inventory with the potentially damaged tool
        itemHandler.setStackInSlot(0, tool);
      } else {
        // For non-tools, try to use the item normally
        Vec3 targetVec = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(targetVec, facing.getOpposite(), targetPos, false);
        tool.useOn(new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND, hitResult));
        itemHandler.setStackInSlot(0, fakePlayer.getItemInHand(InteractionHand.MAIN_HAND));
      }
    }
  }

  private boolean isGrowingPlant(BlockState state) {
    // Check if the block is a sapling
    if (state.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock) {
      return true;
    }

    // Check for crops and other growing plants
    if (state.getBlock() instanceof net.minecraft.world.level.block.CropBlock crop) {
      return !crop.isMaxAge(state);
    }

    // Check for sweet berry bush
    if (state.getBlock() instanceof net.minecraft.world.level.block.SweetBerryBushBlock) {
      return state.getValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE) < 3;
    }

    // Check for bamboo
    if (state.getBlock() instanceof net.minecraft.world.level.block.BambooSaplingBlock) {
      return true;
    }

    // Check for cactus and sugar cane (optional - they can be broken at any stage)
    // if (state.getBlock() instanceof net.minecraft.world.level.block.CactusBlock
    // ||
    // state.getBlock() instanceof net.minecraft.world.level.block.SugarCaneBlock) {
    // return true;
    // }

    return false;
  }

  public void dropInventory(Level level, BlockPos pos) {
    for (int i = 0; i < itemHandler.getSlots(); i++) {
      ItemStack stack = itemHandler.getStackInSlot(i);
      if (!stack.isEmpty()) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
        level.addFreshEntity(itemEntity);
        itemHandler.setStackInSlot(i, ItemStack.EMPTY);
      }
    }
  }
}
