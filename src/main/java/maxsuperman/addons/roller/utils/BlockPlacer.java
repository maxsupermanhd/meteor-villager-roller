package maxsuperman.addons.roller.utils;

import net.minecraft.util.math.BlockPos;

/**
 * Interface to abstract Baritone and Meteor client block placing and breaking.
 */
public interface BlockPlacer {
    /**
     * Place a block at a specific position
     *
     * @param pos    The position to place the block at
     * @param rotate Whether to rotate the player (not used by Baritone)
     * @return Whether the block was placed
     */
    boolean placeBlock(BlockPos pos, boolean rotate);

    /**
     * Break a block at a specific position
     *
     * @param pos The position to break the block at
     * @return Whether the block was broken
     */
    boolean breakBlock(BlockPos pos);

    /**
     * Look at a specific yaw and pitch
     *
     * @param yaw   The yaw to look at
     * @param pitch The pitch to look at
     */
    void lookAt(float yaw, float pitch);
}
