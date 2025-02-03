package com.early_factory.menu;

import com.early_factory.EarlyFactory;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
  public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
      EarlyFactory.MODID);

  public static final RegistryObject<MenuType<BreakerMenu>> BREAKER_MENU = MENUS.register("breaker_menu",
      () -> IForgeMenuType.create(BreakerMenu::new));
}
