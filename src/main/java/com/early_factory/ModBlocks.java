package com.early_factory;

import com.early_factory.block.BreakerBlock;
import com.early_factory.block.CollectorBlock;
import com.early_factory.block.PlacerBlock;

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

  // Breaker
  public static final RegistryObject<Block> BREAKER = BLOCKS.register("breaker",
      () -> new BreakerBlock());

  public static final RegistryObject<Item> BREAKER_ITEM = ModItems.ITEMS.register("breaker",
      () -> new BlockItem(BREAKER.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

  // Placer
  public static final RegistryObject<Block> PLACER = BLOCKS.register("placer", PlacerBlock::new);
  public static final RegistryObject<Item> PLACER_ITEM = ModItems.ITEMS.register("placer",
      () -> new BlockItem(PLACER.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

  // Collector
  public static final RegistryObject<Block> COLLECTOR = BLOCKS.register("collector",
      () -> new CollectorBlock());

  public static final RegistryObject<Item> COLLECTOR_ITEM = ModItems.ITEMS.register("collector",
      () -> new BlockItem(COLLECTOR.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

}
