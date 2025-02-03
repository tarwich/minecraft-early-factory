package com.early_factory.block.entity;

import com.early_factory.block.BreakerBlock;
import com.early_factory.menu.BreakerMenu;

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

public class BreakerBlockEntity extends BlockEntity implements MenuProvider {
  private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
    @Override
    protected void onContentsChanged(int slot) {
      setChanged();
    }
  };

  private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

  private FakePlayer fakePlayer;

  public BreakerBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.BREAKER.get(), pos, state);
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
    return Component.translatable("block.early_factory.breaker");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new BreakerMenu(id, inventory, this);
  }

  public void tick(Level level, BlockPos pos, BlockState state) {
    if (level.isClientSide()) {
      return;
    }

    // Initialize fakePlayer if needed
    if (fakePlayer == null) {
      fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) level);
    }

    Direction facing = state.getValue(BreakerBlock.FACING);
    BlockPos targetPos = pos.relative(facing);

    ItemStack tool = itemHandler.getStackInSlot(0);
    if (!tool.isEmpty()) {
      // Use our cached fake player
      fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, tool.copy());

      // Position the fake player behind the breaker block
      BlockPos playerPos = pos.relative(facing.getOpposite());
      fakePlayer.setPos(playerPos.getX() + 0.5, playerPos.getY() + 0.5, playerPos.getZ() + 0.5);

      // Get the target block state
      BlockState targetState = level.getBlockState(targetPos);
      if (!targetState.isAir()) {
        // Try to break the block if it's a tool
        if (tool.getItem() instanceof DiggerItem) {
          // Start breaking the block
          targetState.attack(level, targetPos, fakePlayer);

          // Actually break the block
          level.destroyBlock(targetPos, true, fakePlayer);

          // Damage the tool
          tool.hurtAndBreak(1, fakePlayer, (player) -> {
          });

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
  }
}
