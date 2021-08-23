package meteordevelopment.addons.roller.modules;

import meteordevelopment.addons.roller.VillagerRollerAddon;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class VillagerRoller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
		.name("auto-switch")
		.description("Automatically switches to the best tool when the block is ready to be mined instantly.")
		.defaultValue(false)
		.build()
	);

	public enum State {
		Disabled,
		WaitingForTargetBlock,
		WaitingForTargetVillager,
		RollingBreakingBlock,
		RollingWaitingForVillagerProfession,
		RollingWaitingForVillagerScreen
	};

	public State CurrentState = State.Disabled;

	public BlockPos RollingBlock;
	public Entity RollingVillager;

	public VillagerRoller() {super(Categories.Misc, "villager-roller", "Rolls trades.");}

	@Override
	public void onActivate() {
		CurrentState = State.WaitingForTargetBlock;
		info("Attack block you want to roll");
	}
	@Override
	public void onDeactivate() {
		CurrentState = State.Disabled;
		info("Roller disabled.");
	}

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if(CurrentState == State.WaitingForTargetBlock) {
            RollingBlock = event.blockPos;
            CurrentState = State.WaitingForTargetVillager;
            info("Rolling block selected, now interact with villager you want to roll");
            event.cancel();
        }
    }

//	@EventHandler
//	private void onTick(TickEvent.Pre event) {
//		// mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
//	}
}


