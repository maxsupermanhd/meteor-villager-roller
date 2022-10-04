package maxsuperman.addons.roller.mixins;

import maxsuperman.addons.roller.modules.VillagerRoller;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
class VillagerInteractMixin {
    @Inject(at = @At("HEAD"), method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", cancellable = true)
    public void interactMob(CallbackInfoReturnable<ActionResult> cir) {
        VillagerRoller roller = Modules.get().get(VillagerRoller.class);
        if (VillagerRoller.currentState == VillagerRoller.State.WaitingForTargetVillager) {
            if(roller.rollingVillager.size()==VillagerRoller.VillagerCount) {
                VillagerRoller.currentState = VillagerRoller.State.RollingBreakingBlock;
                VillagerRoller.pathToBlockPos(roller.standingBlockpos.get(0));
            }
            if(roller.rollingVillager.size()<VillagerRoller.VillagerCount) {
                roller.rollingVillager.add((VillagerEntity) (Object) this);
                roller.info("Villager N " + roller.rollingVillager.size());
                roller.info("We got your villager");
                roller.info("Attack next rolling block");
                VillagerRoller.currentState = VillagerRoller.State.WaitingForTargetBlock;
            }
            cir.setReturnValue(ActionResult.CONSUME);
            cir.cancel();
        }
//        if(cir.isCancelled()) {
//            roller.info("Canceled on interact mixin");
//        }
    }
}
