package com.early_factory;

import com.early_factory.item.MiningPipeItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
      EarlyFactory.MOD_ID);

  public static final RegistryObject<Item> GEAR = ITEMS.register("gear",
      () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS)));

  public static final RegistryObject<Item> LEFT_CLICKER = ITEMS.register("left_clicker",
      () -> new BlockItem(ModBlocks.LEFT_CLICKER_BLOCK.get(),
          new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

  public static final RegistryObject<Item> RIGHT_CLICKER = ITEMS.register("right_clicker",
      () -> new BlockItem(ModBlocks.RIGHT_CLICKER.get(),
          new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

  public static final RegistryObject<Item> COLLECTOR_ITEM = ITEMS.register("collector",
      () -> new BlockItem(ModBlocks.COLLECTOR.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

  public static final RegistryObject<Item> MINER_ITEM = ITEMS.register("miner",
      () -> new BlockItem(ModBlocks.MINER.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

  public static final RegistryObject<Item> WOODEN_MINING_PIPE = ITEMS.register("wooden_mining_pipe",
      () -> new MiningPipeItem(new Item.Properties()));

  public static final RegistryObject<Item> STONE_MINING_PIPE = ITEMS.register("stone_mining_pipe",
      () -> new MiningPipeItem(new Item.Properties()));

  public static final RegistryObject<Item> IRON_MINING_PIPE = ITEMS.register("iron_mining_pipe",
      () -> new MiningPipeItem(new Item.Properties()));

  public static final RegistryObject<Item> DIAMOND_MINING_PIPE = ITEMS.register("diamond_mining_pipe",
      () -> new MiningPipeItem(new Item.Properties()));

  public static final RegistryObject<Item> PIPE_ITEM = ITEMS.register("pipe",
          () -> new BlockItem(ModBlocks.PIPE.get(),
                  new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));
}
