package com.early_factory.pipe;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.level.Level;

public class NetworkManagerProvider {
  private static final Map<Level, NetworkManager> managers = new HashMap<>();

  public static NetworkManager get(Level level) {
    return managers.computeIfAbsent(level, NetworkManager::new);
  }

  public static void remove(Level level) {
    managers.remove(level);
  }
}
