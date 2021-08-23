package maxsuperman.addons.roller.modules;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

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

	public State currentState = State.Disabled;

	public BlockPos rollingBlock;
	public Entity rollingVillager;

	public VillagerRoller() {super(Categories.Misc, "villager-roller", "Rolls trades.");}

	@Override
	public void onActivate() {
		currentState = State.WaitingForTargetBlock;
		info("Attack block you want to roll");
	}
	@Override
	public void onDeactivate() {
		currentState = State.Disabled;
		info("Roller disabled.");
	}

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if(currentState == State.WaitingForTargetBlock) {
            rollingBlock = event.blockPos;
            currentState = State.WaitingForTargetVillager;
            info("Rolling block selected, now interact with villager you want to roll");
            event.cancel();
        }
    }

//	@EventHandler
//	private void onTick(TickEvent.Pre event) {
//		// mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
//	}
}


