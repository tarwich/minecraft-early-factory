package com.early_factory;

import com.early_factory.block.CollectorBlock;
import com.early_factory.block.LeftClickerBlock;
import com.early_factory.block.MinerBlock;
import com.early_factory.block.PipeBlock;
import com.early_factory.block.RightClickerBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
  public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
      EarlyFactory.MOD_ID);

  // Left Clicker
  public static final RegistryObject<Block> LEFT_CLICKER_BLOCK = BLOCKS.register("left_clicker",
      () -> new LeftClickerBlock());

  // Right Clicker
  public static final RegistryObject<Block> RIGHT_CLICKER = BLOCKS.register("right_clicker",
      RightClickerBlock::new);

  // Collector
  public static final RegistryObject<Block> COLLECTOR = BLOCKS.register("collector",
      () -> new CollectorBlock());

  // Miner
  public static final RegistryObject<Block> MINER = BLOCKS.register("miner",
      MinerBlock::new);

  // Pipe
  public static final RegistryObject<Block> PIPE = BLOCKS.register("pipe",
      () -> new PipeBlock(BlockBehaviour.Properties.of(Material.METAL)
          .strength(2.0f)
          .requiresCorrectToolForDrops()
          .noOcclusion()));

}
