package com.early_factory;

import com.early_factory.block.entity.LeftClickerBlockEntity;
import com.early_factory.block.entity.CollectorBlockEntity;
import com.early_factory.block.entity.RightClickerBlockEntity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
      .create(ForgeRegistries.BLOCK_ENTITY_TYPES, EarlyFactory.MODID);

  public static final RegistryObject<BlockEntityType<LeftClickerBlockEntity>> LEFT_CLICKER = BLOCK_ENTITIES
          .register("left_clicker",
                  () -> BlockEntityType.Builder.of(LeftClickerBlockEntity::new,
                          ModBlocks.LEFT_CLICKER_BLOCK.get()).build(null));

  public static final RegistryObject<BlockEntityType<RightClickerBlockEntity>> RIGHT_CLICKER = BLOCK_ENTITIES
          .register("right_clicker", () -> BlockEntityType.Builder.of(RightClickerBlockEntity::new,
                  ModBlocks.RIGHT_CLICKER.get()).build(null));

  public static final RegistryObject<BlockEntityType<CollectorBlockEntity>> COLLECTOR = BLOCK_ENTITIES
      .register("collector", () -> BlockEntityType.Builder.of(CollectorBlockEntity::new,
          ModBlocks.COLLECTOR.get()).build(null));
}
