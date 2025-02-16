package com.early_factory.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.HashMap;

public class MiningTiers {
  private static final Map<ResourceLocation, Integer> TOOL_MINING_LEVELS = new HashMap<>();
  private static final Map<ResourceLocation, Integer> BLOCK_MINING_LEVELS = new HashMap<>();

  static {
    // Initialize tool mining levels - these map to vanilla pickaxe tiers
    TOOL_MINING_LEVELS.put(new ResourceLocation("early_factory", "wooden_mining_pipe"), 0); // Wood tier
    TOOL_MINING_LEVELS.put(new ResourceLocation("early_factory", "stone_mining_pipe"), 1); // Stone tier
    TOOL_MINING_LEVELS.put(new ResourceLocation("early_factory", "iron_mining_pipe"), 2); // Iron tier
    TOOL_MINING_LEVELS.put(new ResourceLocation("early_factory", "diamond_mining_pipe"), 3); // Diamond tier

    // Initialize block mining levels by checking against vanilla pickaxes
    initializeBlockLevels();
  }

  private static void initializeBlockLevels() {
    // Create our test tools
    ItemStack[] woodenTools = { new ItemStack(Items.WOODEN_PICKAXE), new ItemStack(Items.WOODEN_SHOVEL) };
    ItemStack[] stoneTools = { new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.STONE_SHOVEL) };
    ItemStack[] ironTools = { new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_SHOVEL) };
    ItemStack[] diamondTools = { new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.DIAMOND_SHOVEL) };

    // Test blocks against each tier until we find one that can mine it
    for (Block block : net.minecraft.core.Registry.BLOCK) {
      if (!isValidBlock(block.defaultBlockState()))
        continue;

      BlockState state = block.defaultBlockState();
      ResourceLocation blockId = net.minecraft.core.Registry.BLOCK.getKey(block);

      if (canAnyToolMine(woodenTools, state)) {
        BLOCK_MINING_LEVELS.put(blockId, 0);
      } else if (canAnyToolMine(stoneTools, state)) {
        BLOCK_MINING_LEVELS.put(blockId, 1);
      } else if (canAnyToolMine(ironTools, state)) {
        BLOCK_MINING_LEVELS.put(blockId, 2);
      } else if (canAnyToolMine(diamondTools, state)) {
        BLOCK_MINING_LEVELS.put(blockId, 3);
      }
    }
  }

  private static boolean canAnyToolMine(ItemStack[] tools, BlockState state) {
    for (ItemStack tool : tools) {
      if (tool.getItem() instanceof TieredItem tieredItem && tieredItem.isCorrectToolForDrops(tool, state)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isValidBlock(BlockState state) {
    if (state.getFluidState().isEmpty()) {
      Block block = state.getBlock();
      return block instanceof net.minecraft.world.level.block.GravelBlock ||
          block instanceof net.minecraft.world.level.block.SandBlock ||
          block.defaultBlockState().is(BlockTags.BASE_STONE_OVERWORLD) ||
          block.defaultBlockState().is(BlockTags.BASE_STONE_NETHER) ||
          block.defaultBlockState().is(BlockTags.DIRT) ||
          block.defaultBlockState().is(BlockTags.COAL_ORES) ||
          block.defaultBlockState().is(BlockTags.IRON_ORES) ||
          block.defaultBlockState().is(BlockTags.GOLD_ORES) ||
          block.defaultBlockState().is(BlockTags.DIAMOND_ORES) ||
          block.defaultBlockState().is(BlockTags.REDSTONE_ORES) ||
          block.defaultBlockState().is(BlockTags.LAPIS_ORES) ||
          block.defaultBlockState().is(BlockTags.EMERALD_ORES) ||
          block.defaultBlockState().is(BlockTags.COPPER_ORES) ||
          block instanceof net.minecraft.world.level.block.GrassBlock;
    }
    return false;
  }

  public static boolean canMineBlock(Block block, ItemStack pipeStack) {
    if (pipeStack.isEmpty())
      return false;

    ResourceLocation toolType = net.minecraft.core.Registry.ITEM.getKey(pipeStack.getItem());
    int toolLevel = TOOL_MINING_LEVELS.getOrDefault(toolType, 0);

    ResourceLocation blockId = net.minecraft.core.Registry.BLOCK.getKey(block);
    int blockLevel = BLOCK_MINING_LEVELS.getOrDefault(blockId, 1);

    return toolLevel >= blockLevel;
  }

  public static int getToolLevel(ItemStack pipeStack) {
    if (pipeStack.isEmpty())
      return -1;
    ResourceLocation toolType = net.minecraft.core.Registry.ITEM.getKey(pipeStack.getItem());
    return TOOL_MINING_LEVELS.getOrDefault(toolType, 0);
  }

  public static int getBlockLevel(Block block) {
    ResourceLocation blockId = net.minecraft.core.Registry.BLOCK.getKey(block);
    return BLOCK_MINING_LEVELS.getOrDefault(blockId, 1);
  }
}
