package maxsuperman.addons.roller.utils;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.schematic.FillSchematic;
import baritone.api.schematic.ISchematic;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.Rotation;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class BaritoneBlockPlacer implements BlockPlacer {
    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

    @Override
    public boolean placeBlock(BlockPos pos, boolean rotate) {
        baritone.getPathingBehavior().cancelEverything();

        BetterBlockPos lecternPos = BetterBlockPos.from(pos);
        BlockOptionalMeta type = new BlockOptionalMeta(Blocks.LECTERN);
        ISchematic schematic = new FillSchematic(1, 1, 1, type);
        baritone.getBuilderProcess().build("Fill", schematic, lecternPos);
        return true;
    }

    @Override
    public boolean breakBlock(BlockPos pos) {
        baritone.getPathingBehavior().cancelEverything();

        BetterBlockPos lecternPos = BetterBlockPos.from(pos);
        BlockOptionalMeta type = new BlockOptionalMeta(Blocks.AIR);
        ISchematic schematic = new FillSchematic(1, 1, 1, type);
        baritone.getBuilderProcess().build("Fill", schematic, lecternPos);
        return true;
    }

    @Override
    public void lookAt(float yaw, float pitch) {
        IPathingBehavior pathingBehavior = baritone.getPathingBehavior();
        pathingBehavior.cancelEverything();
        pathingBehavior.forceCancel();

        Rotation rotation = new Rotation(yaw, pitch);
        baritone.getLookBehavior().updateTarget(rotation, true);
    }
}
