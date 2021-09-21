package maxsuperman.addons.roller.mixins;

import maxsuperman.addons.roller.modules.VillagerRoller;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreenHandler.class)
class MerchantScreenHandlerMixin {
    @Inject(at = @At("TAIL"), method = "setOffers(Lnet/minecraft/village/TradeOfferList;)V", cancellable = false)
    public void setOffers(TradeOfferList offers, CallbackInfo ci) {
        VillagerRoller roller = Modules.get().get(VillagerRoller.class);
        roller.triggerTradeCheck(offers);
//        roller.info("Merchant mixin");
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
