package com.early_factory;

import com.early_factory.menu.LeftClickerMenu;
import com.early_factory.menu.CollectorMenu;
import com.early_factory.menu.RightClickerMenu;
import com.early_factory.menu.MinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
  public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
                  EarlyFactory.MOD_ID);

  public static final RegistryObject<MenuType<LeftClickerMenu>> LEFT_CLICKER_MENU = MENUS.register("left_clicker_menu",
          () -> IForgeMenuType.create(LeftClickerMenu::new));

  public static final RegistryObject<MenuType<RightClickerMenu>> RIGHT_CLICKER_MENU = MENUS.register(
          "right_clicker_menu",
          () -> IForgeMenuType.create(RightClickerMenu::new));

  public static final RegistryObject<MenuType<CollectorMenu>> COLLECTOR_MENU = MENUS.register("collector_menu",
      () -> IForgeMenuType.create(CollectorMenu::new));

  public static final RegistryObject<MenuType<MinerMenu>> MINER_MENU = MENUS.register("miner_menu",
                  () -> IForgeMenuType.create((windowId, inv, data) -> new MinerMenu(windowId, inv, data)));
}
