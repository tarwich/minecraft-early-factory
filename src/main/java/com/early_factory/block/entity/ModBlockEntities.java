package com.early_factory.block.entity;

import com.early_factory.EarlyFactory;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, EarlyFactory.MODID);

    public static final RegistryObject<BlockEntityType<BreakerBlockEntity>> BREAKER = BLOCK_ENTITIES.register("breaker",
            () -> BlockEntityType.Builder.of(BreakerBlockEntity::new,
                    EarlyFactory.BREAKER.get()).build(null));
}
