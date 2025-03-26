package maxsuperman.addons.roller.utils;

import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DefaultBlockPlacer implements BlockPlacer {
    @Override
    public boolean placeBlock(BlockPos pos, boolean rotate) {
        FindItemResult itemResult = InvUtils.findInHotbar(Blocks.LECTERN.asItem());
        if (!itemResult.found()) return false;
        if (!BlockUtils.canPlace(pos, true)) return false;
        return BlockUtils.place(pos, itemResult, rotate, 5);
    }

    @Override
    public boolean breakBlock(BlockPos pos) {
        return BlockUtils.breakBlock(pos, true);
    }

    @Override
    public void lookAt(float yaw, float pitch) {
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
}
