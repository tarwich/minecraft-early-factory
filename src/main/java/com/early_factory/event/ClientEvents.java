package com.early_factory.event;

import com.early_factory.EarlyFactory;
import com.early_factory.ModMenuTypes;
import com.early_factory.screen.LeftClickerScreen;
import com.early_factory.screen.CollectorScreen;
import com.early_factory.screen.RightClickerScreen;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EarlyFactory.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
      MenuScreens.register(ModMenuTypes.LEFT_CLICKER_MENU.get(), LeftClickerScreen::new);
      MenuScreens.register(ModMenuTypes.COLLECTOR_MENU.get(), CollectorScreen::new);
      MenuScreens.register(ModMenuTypes.RIGHT_CLICKER_MENU.get(), RightClickerScreen::new);
    });
  }
}
