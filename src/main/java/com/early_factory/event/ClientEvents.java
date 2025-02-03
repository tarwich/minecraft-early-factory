package com.early_factory.event;

import com.early_factory.EarlyFactory;
import com.early_factory.menu.ModMenuTypes;
import com.early_factory.screen.BreakerScreen;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EarlyFactory.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
  @SubscribeEvent
  public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
      MenuScreens.register(ModMenuTypes.BREAKER_MENU.get(), BreakerScreen::new);
    });
  }
}
