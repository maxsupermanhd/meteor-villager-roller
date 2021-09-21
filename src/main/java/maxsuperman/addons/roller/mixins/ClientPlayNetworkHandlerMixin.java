package maxsuperman.addons.roller.mixins;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
class ClientPlayNetworkHandlerMixin {
    @Inject(at = @At("HEAD"), method = "onEntityAttributes(Lnet/minecraft/network/packet/s2c/play/EntityAttributesS2CPacket;)V", cancellable = true)
    public void onEntityAttributes(EntityAttributesS2CPacket packet, CallbackInfo ci) {
//        VillagerRoller roller = Modules.get().get(VillagerRoller.class);
//        if(roller.rollingVillager.getId() == packet.getEntityId()) {
//            if (roller.currentState == VillagerRoller.State.RollingWaitingForVillagerProfessionClear) {
//                if(roller.rollingVillager.getVillagerData().getProfession() == VillagerProfession.NONE) {
//                    roller.currentState = VillagerRoller.State.RollingWaitingForVillagerProfessionNew;
//                    roller.info("Our villager changed profession to none");
//                }
//            } else if(roller.currentState == VillagerRoller.State.RollingWaitingForVillagerProfessionNew) {
//                if(roller.rollingVillager.getVillagerData().getProfession() != VillagerProfession.NONE) {
//                    roller.triggerInteract();
//                }
//            }
//        }
    }
}
