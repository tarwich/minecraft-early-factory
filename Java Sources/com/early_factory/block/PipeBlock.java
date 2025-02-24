public class PipeBlock extends Block {
  private static final ResourceLocation TEXTURE_NORMAL = new ResourceLocation("early_factory:block/pipe/normal");
  private static final ResourceLocation TEXTURE_INPUT = new ResourceLocation("early_factory:block/pipe/input");
  private static final ResourceLocation TEXTURE_OUTPUT = new ResourceLocation("early_factory:block/pipe/output");

  @Override
  public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
      BlockState queryState, BlockPos queryPos) {

    if (!(level instanceof Level))
      return state;

    NetworkManager networkManager = getNetworkManager((Level) level);
    PipeNetwork network = networkManager.findNetworkForPipe(pos);

    if (network != null) {
      String mode = network.getInventoryMode(pos, side);
      switch (mode) {
        case "INPUT":
          return state.setValue(TEXTURE, TEXTURE_INPUT);
        case "OUTPUT":
          return state.setValue(TEXTURE, TEXTURE_OUTPUT);
        default:
          return state.setValue(TEXTURE, TEXTURE_NORMAL);
      }
    }

    return state.setValue(TEXTURE, TEXTURE_NORMAL);
  }
}
