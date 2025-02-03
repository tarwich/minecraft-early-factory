package com.early_factory.block.entity;

import com.early_factory.block.PlacerBlock;
import com.early_factory.menu.PlacerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
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

public class PlacerBlockEntity extends BlockEntity implements MenuProvider {
  private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
    @Override
    protected void onContentsChanged(int slot) {
      setChanged();
    }
  };

  private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
  private FakePlayer fakePlayer;

  public PlacerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.PLACER.get(), pos, state);
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    itemHandler.deserializeNBT(tag.getCompound("inventory"));
  }

  @Override
  protected void saveAdditional(CompoundTag tag) {
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
    return Component.translatable("block.early_factory.placer");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new PlacerMenu(id, inventory, this);
  }

  public void tick(Level level, BlockPos pos, BlockState state) {
    if (level.isClientSide()) {
      return;
    }

    // Initialize fakePlayer if needed
    if (fakePlayer == null) {
      fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) level);
    }

    Direction facing = state.getValue(PlacerBlock.FACING);
    BlockPos targetPos = pos.relative(facing);
    BlockState targetState = level.getBlockState(targetPos);

    ItemStack stack = itemHandler.getStackInSlot(0);
    if (!stack.isEmpty()) {
      // Position the fake player behind the placer block
      BlockPos playerPos = pos.relative(facing.getOpposite());
      fakePlayer.setPos(playerPos.getX() + 0.5, playerPos.getY() + 0.5, playerPos.getZ() + 0.5);

      // Set the item in the fake player's hand
      ItemStack useStack = stack.copy();
      fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, useStack);

      // Create hit result for the target position
      Vec3 targetVec = new Vec3(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5);
      BlockHitResult hitResult = new BlockHitResult(targetVec, facing.getOpposite(), targetPos, false);

      // Try to use/place the item
      UseOnContext context = new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND, hitResult);

      // If it's a block item and the target space is empty, try to place it
      if (stack.getItem() instanceof BlockItem && targetState.isAir()) {
        stack.useOn(context);
      }
      // For non-block items (like bonemeal), try to use them on the target block
      else if (!targetState.isAir()) {
        stack.useOn(context);
      }

      // Update the inventory with the potentially used item
      itemHandler.setStackInSlot(0, fakePlayer.getItemInHand(InteractionHand.MAIN_HAND));
    }
  }
}
