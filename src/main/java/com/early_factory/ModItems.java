package com.early_factory;

import com.early_factory.item.MiningPipeItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
      EarlyFactory.MOD_ID);

  public static final RegistryObject<Item> WOODEN_MINING_PIPE = ITEMS.register("wooden_mining_pipe",
      () -> new MiningPipeItem(new Item.Properties()));

  public static final RegistryObject<Item> STONE_MINING_PIPE = ITEMS.register("stone_mining_pipe",
      () -> new MiningPipeItem(new Item.Properties()));

  public static final RegistryObject<Item> IRON_MINING_PIPE = ITEMS.register("iron_mining_pipe",
      () -> new MiningPipeItem(new Item.Properties()));

  public static final RegistryObject<Item> DIAMOND_MINING_PIPE = ITEMS.register("diamond_mining_pipe",
      () -> new MiningPipeItem(new Item.Properties()));
}
