package com.early_factory.block.entity;

import javax.annotation.Nonnull;

import com.early_factory.ModBlockEntities;
import com.early_factory.block.RightClickerBlock;
import com.early_factory.menu.RightClickerMenu;

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
import net.minecraft.world.item.BlockItem;
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

public class RightClickerBlockEntity extends BlockEntity implements MenuProvider {
  private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
    @Override
    protected void onContentsChanged(int slot) {
      setChanged();
    }
  };

  private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
  private FakePlayer fakePlayer;

  public RightClickerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.RIGHT_CLICKER.get(), pos, state);
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
    return Component.translatable("block.early_factory.right_clicker");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
    return new RightClickerMenu(id, inventory, this);
  }

  public void tick(Level level, BlockPos pos, BlockState state) {
    if (level.isClientSide()) {
      return;
    }

    // Initialize fakePlayer if needed
    if (fakePlayer == null) {
      fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) level);
    }

    Direction facing = state.getValue(RightClickerBlock.FACING);
    BlockPos targetPos = pos.relative(facing);
    BlockState targetState = level.getBlockState(targetPos);

    // Position the fake player behind the clicker block
    BlockPos playerPos = pos.relative(facing.getOpposite());
    fakePlayer.setPos(playerPos.getX() + 0.5, playerPos.getY() + 0.5, playerPos.getZ() + 0.5);

    ItemStack stack = itemHandler.getStackInSlot(0);
    // Set the item in the fake player's hand (empty or not)
    fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());

    // Create hit result for the target position
    Vec3 targetVec = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
    BlockHitResult hitResult = new BlockHitResult(targetVec, facing.getOpposite(), targetPos, false);

    if (!targetState.isAir()) {
      // Try to interact with the block first (like pressing a button)
      targetState.use(level, fakePlayer, InteractionHand.MAIN_HAND, hitResult);
    } else if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
      // If we have a block item and the target space is empty, try to place it
      UseOnContext context = new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND, hitResult);
      stack.useOn(context);
    }

    // Update the inventory with the potentially used item
    itemHandler.setStackInSlot(0, fakePlayer.getItemInHand(InteractionHand.MAIN_HAND));
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
