package maxsuperman.addons.roller.mixins;

import maxsuperman.addons.roller.modules.VillagerRoller;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
class VillagerInteractMixin {
    @Inject(at = @At("HEAD"), method = "interactMob(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", cancellable = true)
    public void interactMob(CallbackInfoReturnable<ActionResult> cir) {
        VillagerRoller roller = Modules.get().get(VillagerRoller.class);
        if (roller.currentState == VillagerRoller.State.WaitingForTargetVillager) {
            roller.rollingVillager = (VillagerEntity) (Object) this;
            roller.info("We got your villager");
            roller.currentState = VillagerRoller.State.RollingBreakingBlock;
            cir.setReturnValue(ActionResult.CONSUME);
            cir.cancel();
        }
//        if(cir.isCancelled()) {
//            roller.info("Canceled on interact mixin");
//        }
    }
}
