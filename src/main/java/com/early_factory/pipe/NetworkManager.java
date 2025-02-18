package com.early_factory.pipe;

import java.util.HashSet;
import java.util.Set;

import com.early_factory.block.PipeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class NetworkManager {
  private final Set<PipeNetwork> networks = new HashSet<>();
  private final Level level;

  public NetworkManager(Level level) {
    this.level = level;
  }

  public void onPipePlaced(BlockPos pos) {
    // Find a single adjacent pipe and its network
    PipeNetwork existingNetwork = null;

    for (Direction dir : Direction.values()) {
      BlockPos neighborPos = pos.relative(dir);
      Block neighborBlock = level.getBlockState(neighborPos).getBlock();

      if (neighborBlock instanceof PipeBlock) {
        existingNetwork = findNetworkForPipe(neighborPos);
        if (existingNetwork != null) {
          break;
        }
      }
    }

    if (existingNetwork != null) {
      // Add to existing network
      existingNetwork.addPipe(pos);
    } else {
      // Create new network with a single pipe
      Set<BlockPos> initialPipes = new HashSet<>();
      initialPipes.add(pos);
      networks.add(new PipeNetwork(level, initialPipes));
    }
  }

  public void onPipeRemoved(BlockPos pos) {
    PipeNetwork network = findNetworkForPipe(pos);
    if (network != null) {
      // Remove the pipe first
      network.removePipe(pos);

      if (!network.getPipes().isEmpty()) {
        // Check if network needs to split
        Set<BlockPos> remainingPipes = network.getPipes();
        Set<BlockPos> connectedPipes = new HashSet<>();

        // Start flood fill from any remaining pipe
        BlockPos startPipe = remainingPipes.iterator().next();
        floodFillNetwork(startPipe, connectedPipes, remainingPipes);

        if (connectedPipes.size() < remainingPipes.size()) {
          // Network is split
          Set<BlockPos> disconnectedPipes = new HashSet<>(remainingPipes);
          disconnectedPipes.removeAll(connectedPipes);

          // Update existing network to only contain connected pipes
          network.resetPipes(connectedPipes);

          // Create new network for disconnected pipes
          networks.add(new PipeNetwork(level, disconnectedPipes));
        }
      } else {
        // Network is empty, remove it
        networks.remove(network);
      }
    }
  }

  private void floodFillNetwork(BlockPos pos, Set<BlockPos> visited, Set<BlockPos> validPipes) {
    if (!validPipes.contains(pos) || !visited.add(pos)) {
      return;
    }

    for (Direction dir : Direction.values()) {
      floodFillNetwork(pos.relative(dir), visited, validPipes);
    }
  }

  private PipeNetwork findNetworkForPipe(BlockPos pos) {
    for (PipeNetwork network : networks) {
      if (network.getPipes().contains(pos)) {
        return network;
      }
    }
    return null;
  }

  public void tick() {
    for (PipeNetwork network : networks) {
      network.tick();
    }
  }
}
