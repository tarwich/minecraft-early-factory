package com.early_factory;

import com.early_factory.block.LeftClickerBlock;
import com.early_factory.block.CollectorBlock;
import com.early_factory.block.RightClickerBlock;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
  public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
      EarlyFactory.MODID);

  // Left Clicker
  public static final RegistryObject<Block> LEFT_CLICKER_BLOCK = BLOCKS.register("left_clicker",
          () -> new LeftClickerBlock());

  public static final RegistryObject<Item> LEFT_CLICKER_BLOCK_ITEM = ModItems.ITEMS.register("left_clicker",
          () -> new BlockItem(LEFT_CLICKER_BLOCK.get(),
                  new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

  // Right Clicker
  public static final RegistryObject<Block> RIGHT_CLICKER = BLOCKS.register("right_clicker",
          RightClickerBlock::new);
  public static final RegistryObject<Item> RIGHT_CLICKER_ITEM = ModItems.ITEMS.register("right_clicker",
          () -> new BlockItem(RIGHT_CLICKER.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

  // Collector
  public static final RegistryObject<Block> COLLECTOR = BLOCKS.register("collector",
      () -> new CollectorBlock());

  public static final RegistryObject<Item> COLLECTOR_ITEM = ModItems.ITEMS.register("collector",
      () -> new BlockItem(COLLECTOR.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

}
