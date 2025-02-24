package com.early_factory.pipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class PipeNetwork {
  private final Set<BlockPos> pipes = new HashSet<>();
  // Map of inventory position to its handler
  private final Map<BlockPos, IItemHandler> connectedInventories = new HashMap<>();
  // Map of inventory position to the set of pipes that connect to it
  private final Map<BlockPos, Set<BlockPos>> inventoryConnections = new HashMap<>();
  private final Level level;
  // Add these fields to track input/output status
  private final Set<BlockPos> inputInventories = new HashSet<>();
  private final Set<BlockPos> outputInventories = new HashSet<>();

  public PipeNetwork(Level level, Set<BlockPos> pipesToAdd) {
    this.level = level;
    for (BlockPos pos : pipesToAdd) {
      addPipe(pos);
    }
  }

  public void addPipe(BlockPos pos) {
    if (pipes.add(pos)) {
      // If pipe was added, scan for connected inventories
      scanForInventories(pos);
    }
  }

  public void removePipe(BlockPos pos) {
    if (pipes.remove(pos)) {
      // Remove this pipe's connections and clean up orphaned inventories
      removeInventoryConnections(pos);

      // Check if network should split
      if (!pipes.isEmpty()) {
        validateNetwork();
      }
    }
  }

  private void scanForInventories(BlockPos pipePos) {
    for (Direction dir : Direction.values()) {
      BlockPos neighborPos = pipePos.relative(dir);
      BlockEntity neighborEntity = level.getBlockEntity(neighborPos);

      if (neighborEntity != null) {
        neighborEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
          // Add or get the set of pipes connected to this inventory
          Set<BlockPos> connections = inventoryConnections
              .computeIfAbsent(neighborPos, k -> new HashSet<>());
          connections.add(pipePos);

          // Add the inventory if it's not already present
          connectedInventories.putIfAbsent(neighborPos, handler);
        });
      }
    }
  }

  private void removeInventoryConnections(BlockPos pipePos) {
    // Create a list to track inventories that might need removal
    List<BlockPos> inventoriesToCheck = new ArrayList<>();

    // Find all inventories this pipe was connected to
    for (Map.Entry<BlockPos, Set<BlockPos>> entry : inventoryConnections.entrySet()) {
      if (entry.getValue().remove(pipePos)) {
        // If we removed a connection, check if there are any left
        if (entry.getValue().isEmpty()) {
          inventoriesToCheck.add(entry.getKey());
        }
      }
    }

    // Remove inventories that have no more connections
    for (BlockPos invPos : inventoriesToCheck) {
      inventoryConnections.remove(invPos);
      connectedInventories.remove(invPos);
    }
  }

  private void validateNetwork() {
    Set<BlockPos> connectedPipes = new HashSet<>();
    BlockPos startPipe = pipes.iterator().next();
    floodFillNetwork(startPipe, connectedPipes);

    if (connectedPipes.size() != pipes.size()) {
      // Network is split! Handle the split
      Set<BlockPos> disconnectedPipes = new HashSet<>(pipes);
      disconnectedPipes.removeAll(connectedPipes);

      // Update this network to only contain the connected pipes
      pipes.clear();
      pipes.addAll(connectedPipes);

      // Remove inventory connections for disconnected pipes
      for (BlockPos disconnectedPipe : disconnectedPipes) {
        removeInventoryConnections(disconnectedPipe);
      }

      // The NetworkManager will handle creating new network(s) for disconnected pipes
      createNewNetworksFromPipes(disconnectedPipes);
    }
  }

  private void floodFillNetwork(BlockPos pos, Set<BlockPos> visited) {
    if (!visited.add(pos))
      return;

    for (Direction dir : Direction.values()) {
      BlockPos neighborPos = pos.relative(dir);
      if (pipes.contains(neighborPos)) {
        floodFillNetwork(neighborPos, visited);
      }
    }
  }

  private void createNewNetworksFromPipes(Set<BlockPos> disconnectedPipes) {
    // This will be handled by NetworkManager's onPipeRemoved method
    // We don't need to do anything here since NetworkManager will create the new
    // network
  }

  public void tick() {
    // Try to move items from each input inventory
    for (BlockPos inputPos : inputInventories) {
      IItemHandler inputHandler = connectedInventories.get(inputPos);
      if (inputHandler == null)
        continue;

      // Try each slot in the input inventory
      for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
        // Try to extract (simulate first)
        ItemStack extracted = inputHandler.extractItem(slot, 1, true);
        if (extracted.isEmpty())
          continue;

        // Try to find an output that can accept this item
        for (BlockPos outputPos : outputInventories) {
          IItemHandler outputHandler = connectedInventories.get(outputPos);
          if (outputHandler == null)
            continue;

          // Try to insert the item (simulate first)
          ItemStack remaining = ItemHandlerHelper.insertItem(outputHandler, extracted, true);
          if (remaining.isEmpty()) {
            // If simulation succeeded, do the actual transfer
            extracted = inputHandler.extractItem(slot, 1, false);
            ItemHandlerHelper.insertItem(outputHandler, extracted, false);
            break; // Move to next slot after successful transfer
          }
        }
      }
    }
  }

  public Set<BlockPos> getPipes() {
    return new HashSet<>(pipes);
  }

  public Map<BlockPos, IItemHandler> getConnectedInventories() {
    return Collections.unmodifiableMap(connectedInventories);
  }

  public Map<BlockPos, Set<BlockPos>> getInventoryConnections() {
    return Collections.unmodifiableMap(inventoryConnections);
  }

  public void resetPipes(Set<BlockPos> newPipes) {
    // Clear existing pipes and their inventory connections
    for (BlockPos pipe : pipes) {
      removeInventoryConnections(pipe);
    }
    pipes.clear();

    // Add new pipes
    for (BlockPos pos : newPipes) {
      addPipe(pos);
    }
  }

  // Add this method to toggle input/output status (called when player
  // right-clicks a pipe)
  public void toggleInventoryMode(BlockPos pipePos, Direction face) {
    BlockPos inventoryPos = pipePos.relative(face);
    if (connectedInventories.containsKey(inventoryPos)) {
      if (inputInventories.remove(inventoryPos)) {
        // Was input, now make it output
        outputInventories.add(inventoryPos);
      } else if (outputInventories.remove(inventoryPos)) {
        // Was output, now make it neither
      } else {
        // Was neither, make it input
        inputInventories.add(inventoryPos);
      }
    }
  }

  public String getInventoryMode(BlockPos pipePos, Direction face) {
    BlockPos inventoryPos = pipePos.relative(face);
    if (inputInventories.contains(inventoryPos)) {
      return "INPUT";
    } else if (outputInventories.contains(inventoryPos)) {
      return "OUTPUT";
    }
    return "NONE";
  }
}
