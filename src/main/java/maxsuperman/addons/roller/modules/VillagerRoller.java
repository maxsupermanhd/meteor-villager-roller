package maxsuperman.addons.roller.modules;

import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ChatUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK;

public class VillagerRoller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Enchantment>> searchingEnchants = sgGeneral.add(new EnchantmentListSetting.Builder()
        .name("searching-enchants")
        .description("Enchantments to search")
        .defaultValue(Arrays.asList(Enchantment.byRawId(0)))
        .build()
    );

    private final Setting<Boolean> removeIfFound = sgGeneral.add(new BoolSetting.Builder()
        .name("remove-when-found")
        .description("Remove enchantment from list if found")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> enablePlaySound = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-sound")
        .description("Plays sound when it finds desired trade")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<SoundEvent>> sound = sgGeneral.add(new SoundEventListSetting.Builder()
        .name("sound-to-play")
        .description("Sound that will be played when desired trade is found if enabled")
        .defaultValue(Arrays.asList(BLOCK_AMETHYST_CLUSTER_BREAK))
        .build()
    );

    private final Setting<Boolean> enableMaxPrice = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-max-price")
        .description("Check max price")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maxPrice = sgGeneral.add(new IntSetting.Builder()
        .name("max-price")
        .description("Max price")
        .defaultValue(14)
        .min(1)
        .sliderMax(64)
        .build()
    );

//    private final Setting<Boolean> sendInteraction = sgGeneral.add(new BoolSetting.Builder()
//        .name("send-interaction")
//        .description("Actually interact with villager")
//        .defaultValue(true)
//        .build()
//    );

	public enum State {
		Disabled,
		WaitingForTargetBlock,
		WaitingForTargetVillager,
		RollingBreakingBlock,
        RollingWaitingForVillagerProfessionClear,
        RollingPlacingBlock,
        RollingWaitingForVillagerProfessionNew,
        RollingWaitingForVillagerTrades
	}

	public State currentState = State.Disabled;

	public BlockPos rollingBlockPos;
    public Block rollingBlock;
	public VillagerEntity rollingVillager;

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

	public void triggerInteract() {
//	    if(sendInteraction.get()) {
//            info(String.format("Interacting with villager"));
            mc.interactionManager.interactEntity(mc.player, rollingVillager, Hand.MAIN_HAND);
//        } else {
//            info(String.format("Checking trades"));
//            for (Entity entity : mc.world.getEntities()) {
//                if(entity.getUuid().equals(rollingVillager.getUuid())) {
//                    triggerTradeCheck(((VillagerEntity) entity).getOffers());
//                }
//            }
//        }
    }

    public void triggerTradeCheck(TradeOfferList l) {
	    if(currentState != State.RollingWaitingForVillagerTrades) {
	        return;
        }
        for (TradeOffer offer : l) {
//            info(String.format("Offer: %s", offer.getSellItem().toString()));
            Map<Enchantment, Integer> offerEnchants = EnchantmentHelper.get(offer.getSellItem());
            for (Map.Entry<Enchantment, Integer> enchant : offerEnchants.entrySet()) {
                if(enchant.getKey().getMaxLevel() == enchant.getValue() && searchingEnchants.get().contains(enchant.getKey())) {
                    if(enableMaxPrice.get() && offer.getOriginalFirstBuyItem().getCount() > maxPrice.get()) {
                        info(String.format("Found enchant %s %sbut it costs too much: %s (max price) < %d (cost)", ChatUtil.stripTextFormat(new TranslatableText(enchant.getKey().getTranslationKey()).getString()), enchant.getKey().getMaxLevel() == 1 ? "" : enchant.getValue().toString()+" ", maxPrice.get().toString(), offer.getOriginalFirstBuyItem().getCount()));
                        continue;
                    }
                    ChatUtils.sendMsg("Villager Roller found enchantment", enchant.getKey().getName(enchant.getValue()));
                    if(removeIfFound.get()) {
                        searchingEnchants.get().remove(enchant.getKey());
                    }
                    toggle();
                    if(enablePlaySound.get() && sound.get().size() > 0) {
                        mc.getSoundManager().play(PositionedSoundInstance.master(this.sound.get().get(0), 1.0F, 1.0F));
                    }
                    return;
                } else {
                    info(String.format("Found enchant %s %s but it is not in the list.", ChatUtil.stripTextFormat(new TranslatableText(enchant.getKey().getTranslationKey()).getString()), enchant.getValue().toString()));
                }
            }
        }
//        ((MerchantScreenHandler)mc.player.currentScreenHandler).closeHandledScreen();
        mc.player.closeHandledScreen();
        currentState = State.RollingBreakingBlock;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if(currentState == State.WaitingForTargetBlock) {
            rollingBlockPos = event.blockPos;
            rollingBlock = mc.world.getBlockState(rollingBlockPos).getBlock();
            currentState = State.WaitingForTargetVillager;
            info("Rolling block selected, now interact with villager you want to roll");
            event.cancel();
        }
    }

	@EventHandler
	private void onTick(TickEvent.Pre event) {
	    if(currentState == State.RollingBreakingBlock) {
            if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
//                info("Block is broken, waiting for villager to clean profession...");
                currentState = State.RollingWaitingForVillagerProfessionClear;
            } else {
                if (!BlockUtils.breakBlock(rollingBlockPos, true)) {
                    info("Can not break block");
                    toggle();
                }
            }
        } else if(currentState == State.RollingWaitingForVillagerProfessionClear) {
            if(rollingVillager.getVillagerData().getProfession() == VillagerProfession.NONE) {
//                info("Profession cleared");
                currentState = State.RollingPlacingBlock;
            }
        } else if(currentState == State.RollingPlacingBlock) {
//	        info("Placing block");
            FindItemResult item = InvUtils.findInHotbar(rollingBlock.asItem());
            if(!BlockUtils.place(rollingBlockPos, item, 5)) {
                info("Failed to place block");
            }
            currentState = State.RollingWaitingForVillagerProfessionNew;
        } else if(currentState == State.RollingWaitingForVillagerProfessionNew) {
            if(rollingVillager.getVillagerData().getProfession() != VillagerProfession.NONE) {
                currentState = State.RollingWaitingForVillagerTrades;
                triggerInteract();
            }
        }
	}
}


