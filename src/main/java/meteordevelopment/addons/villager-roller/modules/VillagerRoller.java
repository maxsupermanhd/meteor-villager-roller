package meteordevelopment.addons.template.modules;

import meteordevelopment.addons.villager-roller.VillagerRollerAddon;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.util.math;

public class VillagerRoller extends Module {
	private final SettingGroup sgGeneral = settings.getDefaultGroup();
	private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
		.name("auto-switch")
		.description("Automatically switches to the best tool when the block is ready to be mined instantly.")
		.defaultValue(false)
		.build()
	);

	private enum State {
		Disabled,
		WaitingForTargetBlock,
		WaitingForTargetVillager,
		RollingBreakingBlock,
		RollingWaitingForVillagerProfession,
		RollingWaitingForVillagerScreen
	};
	
	private State CurrentState = Disabled;
	
	private BlockPos RollingBlock;
	private Entity RollingVillager;

	public VillagerRoller() {
		super(Categories.Misc, "villager-roller", "Rolls trades.");
	}

	@Override
	public void onActivate() {
		CurrentState = WaitingForTargetBlock;
		info("Attack block you want to roll");
	}
	@Override
	public void onDeactivate() {
		CurrentState = Disabled;
		info("Roller disabled.");
	}

	@EventHandler(priority = EventPriority.HIGH)
	private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
		blocks.get().contains(mc.world.getBlockState(event.blockPos).getBlock())
		if(CurrentState == WaitingForTargetBlock) {
			RollingBlock = 
			event.cancel();
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);
        if (entity == null || !(entity.equals(mc.player))) return;

        ticks = 0;
    }
	
	@EventHandler
	private void onTick(TickEvent.Pre event) {
		// mc.interactionManager.interactEntity(mc.player, entity, offHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
		if(CurrentState == RollingWaitingForVillagerProfession) {
			
		} else if(CurrentState == RollingBreakingBlock) {
			
		}
	}
	
}