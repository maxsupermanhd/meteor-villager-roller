package maxsuperman.addons.roller.utils;

import meteordevelopment.meteorclient.pathing.BaritoneUtils;

public class BlockPlacerFactory {
    private static BlockPlacer baritoneInstance;
    private static BlockPlacer defaultInstance;

    private BlockPlacerFactory() {
    }

    public static BlockPlacer getBlockPlacer(boolean useBaritone) {
        if (BaritoneUtils.IS_AVAILABLE && useBaritone) {
            if (baritoneInstance == null) {
                baritoneInstance = new BaritoneBlockPlacer();
            }
            return baritoneInstance;
        } else {
            if (defaultInstance == null) {
                defaultInstance = new DefaultBlockPlacer();
            }
            return defaultInstance;
        }
    }
}
