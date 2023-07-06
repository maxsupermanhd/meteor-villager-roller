package maxsuperman.addons.roller.modules;

import com.google.common.collect.Maps;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WIntEdit;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Names;
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
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static net.minecraft.enchantment.EnchantmentHelper.getIdFromNbt;
import static net.minecraft.enchantment.EnchantmentHelper.getLevelFromNbt;
import static net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;

public class VillagerRoller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("Sound");

    private final Setting<Boolean> disableIfFound = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-found")
        .description("Disable enchantment from list if found")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> saveListToConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("save-list-to-config")
        .description("Toggles saving and loading of rolling list to config and copypaste buffer")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> enablePlaySound = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-sound")
        .description("Plays sound when it finds desired trade")
        .defaultValue(true)
        .build());

    private final Setting<List<SoundEvent>> sound = sgSound.add(new SoundEventListSetting.Builder()
        .name("sound-to-play")
        .description("Sound that will be played when desired trade is found if enabled")
        .defaultValue(Collections.singletonList(BLOCK_AMETHYST_CLUSTER_BREAK))
        .build());

    private final Setting<Double> soundPitch = sgSound.add(new DoubleSetting.Builder()
        .name("sound-pitch")
        .description("Playing sound pitch")
        .defaultValue(1.0)
        .min(0)
        .sliderRange(0, 8)
        .build());

    private final Setting<Double> soundVolume = sgSound.add(new DoubleSetting.Builder()
        .name("sound-volume")
        .description("Playing sound volume")
        .defaultValue(1.0)
        .min(0)
        .sliderRange(0, 1)
        .build());

    private final Setting<Boolean> pauseOnScreen = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-screens")
        .description("Pauses rolling if any screen is open")
        .defaultValue(true)
        .build());

    private final Setting<Boolean> headRotateOnPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate-place")
        .description("Look to the block while placing it?")
        .defaultValue(true)
        .build());

    private final Setting<Integer> failedToPlaceDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-fail-delay")
        .description("Delay after failed block place (milliseconds)")
        .defaultValue(1500)
        .min(0)
        .sliderRange(0, 10000)
        .build());

    private final Setting<Boolean> failedToPlaceDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("place-fail-disable")
        .description("Disables roller if block placement fails")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> sortEnchantments = sgGeneral.add(new BoolSetting.Builder()
        .name("sort-enchantments")
        .description("Show enchantments sorted by their name")
        .defaultValue(true)
        .build());

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

    public static State currentState = State.Disabled;

    public BlockPos rollingBlockPos;
    public Block rollingBlock;
    public VillagerEntity rollingVillager;
    public List<rollingEnchantment> searchingEnchants = new ArrayList<>();
    private long failedToPlacePrevMsg = System.currentTimeMillis();

    public VillagerRoller() {
        super(Categories.Misc, "villager-roller", "Rolls trades.");
    }

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

    @Override
    public String getInfoString() {
        return currentState.toString();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        if (saveListToConfig.get()) {
            NbtList l = new NbtList();
            for (rollingEnchantment e : searchingEnchants) {
                l.add(e.toTag());
            }
            tag.put("rolling", l);
        }
        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);
        if (saveListToConfig.get()) {
            NbtList l = tag.getList("rolling", NbtElement.COMPOUND_TYPE);
            searchingEnchants.clear();
            for (NbtElement e : l) {
                if (e.getType() != NbtElement.COMPOUND_TYPE) {
                    info("Invalid list element");
                    continue;
                }
                searchingEnchants.add(new rollingEnchantment().fromTag((NbtCompound) e));
            }
        }
        return this;
    }

    public boolean loadSearchingFromFile(File f) {
        if (!f.exists() || !f.canRead()) {
            info("File does not exist or can not be loaded");
            return false;
        }
        NbtCompound r = null;
        try {
            r = NbtIo.read(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (r == null) {
            info("Failed to load nbt from file");
            return false;
        }
        NbtList l = r.getList("rolling", NbtElement.COMPOUND_TYPE);
        searchingEnchants.clear();
        for (NbtElement e : l) {
            if (e.getType() != NbtElement.COMPOUND_TYPE) {
                info("Invalid list element");
                return false;
            }
            searchingEnchants.add(new rollingEnchantment().fromTag((NbtCompound) e));
        }
        return true;
    }

    public boolean saveSearchingToFile(File f) {
        NbtList l = new NbtList();
        for (rollingEnchantment e : searchingEnchants) {
            l.add(e.toTag());
        }
        NbtCompound c = new NbtCompound();
        c.put("rolling", l);
        if (Files.notExists(f.getParentFile().toPath())) {
            if (!f.getParentFile().mkdirs()) {
                info("Failed to make directories");
                return false;
            }
        }
        try {
            NbtIo.write(c, f);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        WSection loadDataSection = list.add(theme.section("Config Saving")).expandX().widget();

        WTable control = loadDataSection.add(theme.table()).expandX().widget();

        WTextBox nfname = control.add(theme.textBox("default")).expandWidgetX().expandCellX().expandX().widget();
        WButton save = control.add(theme.button("Save")).expandX().widget();
        save.action = () -> {
            if (saveSearchingToFile(new File(new File(MeteorClient.FOLDER, "VillagerRoller"), nfname.get()+".nbt"))) {
                info("Saved successfully");
            } else {
                info("Save failed");
            }
            list.clear();
            fillWidget(theme, list);
        };
        control.row();

        ArrayList<String> fnames = new ArrayList<>();
        var path = MeteorClient.FOLDER.toPath().resolve("VillagerRoller");
        if(Files.notExists(path)) {
            if(!path.toFile().mkdirs()) {
                error("Failed to create directory [{}]", path);
            }
        } else {
            try (var l = Files.list(path)) {
                l.forEach(p -> {
                    String name = p.getFileName().toString();
                    fnames.add(name.substring(0, name.length() - 4));
                });
            } catch(IOException e) {
                error("Failed to list directory", e);
            }
        }
        if (fnames.size() != 0) {
            WDropdown<String> lfname = control.add(theme.dropdown(fnames.toArray(new String[0]), "default"))
                .expandWidgetX().expandCellX().expandX().widget();
            WButton load = control.add(theme.button("Load")).expandX().widget();
            load.action = () -> {
                if (loadSearchingFromFile(new File(new File(MeteorClient.FOLDER, "VillagerRoller"), lfname.get()+".nbt"))) {
                    list.clear();
                    fillWidget(theme, list);
                    info("Loaded successfully");
                } else {
                    error("Failed to load file.");
                }
            };
        }

        WSection enchantments = list.add(theme.section("Enchantments")).expandX().widget();

        WTable table = enchantments.add(theme.table()).expandX().widget();
        table.add(theme.item(Items.BOOK.getDefaultStack()));
        table.add(theme.label("Enchantment"));
        table.add(theme.label("Level"));
        table.add(theme.label("Cost"));
        table.add(theme.label("Enabled"));
        table.add(theme.label("Remove"));
        table.row();
        if (sortEnchantments.get()) {
            searchingEnchants.sort(Comparator.comparing(o -> o.enchantment));
        }

        for (int i = 0; i < searchingEnchants.size(); i++) {
            rollingEnchantment e = searchingEnchants.get(i);
            final int si = i;
            ItemStack book = Items.ENCHANTED_BOOK.getDefaultStack();
            int maxlevel = 255;
            if(e.isVanilla()) {
                Enchantment ench = Registries.ENCHANTMENT.get(e.enchantment);
                maxlevel = e.getMaxLevel();
                book.addEnchantment(ench, e.minLevel == 0 ? maxlevel : e.minLevel);
            }
            table.add(theme.item(book));

            WHorizontalList label = theme.horizontalList();
            WButton c = label.add(theme.button("Change")).widget();
            c.action = () -> mc.setScreen(new EnchantmentSelectScreen(theme, (rollingEnchantment sel) -> {
                searchingEnchants.set(si, sel);
                list.clear();
                fillWidget(theme, list);
            }));
            if(e.isCustom()) {
                label.add(theme.label(e.enchantment.toString()));
            } else {
                Registries.ENCHANTMENT.getOrEmpty(e.enchantment).ifPresent(en -> label.add(theme.label(Names.get(en))));
            }
            table.add(label);

            WIntEdit lev = table.add(theme.intEdit(e.minLevel, 0, maxlevel, true)).minWidth(40)
                    .expandX().widget();
            lev.action = () -> e.minLevel = lev.get();
            lev.tooltip = "Minimum enchantment level, 0 acts as maximum possible only (for custom 0 acts like 1)";

            var costbox = table.add(theme.horizontalList()).minWidth(50).expandX().widget();
            WIntEdit cost = costbox.add(theme.intEdit(e.maxCost, 0, 64, false)).minWidth(40).expandX().widget();
            cost.action = () -> e.maxCost = cost.get();
            cost.tooltip = "Maximum cost in emeralds, 0 means no limit";

            var setOptimal = costbox.add(theme.button("O")).widget();
            setOptimal.tooltip = "Set to optimal price (5 + maxLevel*3) (double if treasure) (if known)";
            setOptimal.action = () -> {
                list.clear();
                if(e.isVanilla()) {
                    e.maxCost = getMinimumPrice(Registries.ENCHANTMENT.get(e.enchantment), e.getMaxLevel());
                }
                fillWidget(theme, list);
            };

            WCheckbox en = table.add(theme.checkbox(e.enabled)).widget();
            en.action = () -> e.enabled = en.checked;
            en.tooltip = "Enabled?";

            WMinus del = table.add(theme.minus()).widget();
            del.action = () -> {
                list.clear();
                searchingEnchants.remove(e);
                fillWidget(theme, list);
            };
            table.row();
        }

        WTable controls = list.add(theme.table()).expandX().widget();

        WButton removeAll = controls.add(theme.button("Remove all")).expandX().widget();
        removeAll.action = () -> {
            list.clear();
            searchingEnchants.clear();
            fillWidget(theme, list);
        };

        WButton create = controls.add(theme.button("Add")).expandX().widget();
        create.action = () -> mc.setScreen(new EnchantmentSelectScreen(theme, (e) -> {
            if(e.isVanilla()) {
                e.minLevel = e.getMaxLevel();
                e.maxCost = getMinimumPrice(Registries.ENCHANTMENT.get(e.enchantment), e.minLevel);
            }
            e.enabled = true;
            searchingEnchants.add(e);
            list.clear();
            fillWidget(theme, list);
        }));

        WButton addAll = controls.add(theme.button("Add all")).expandX().widget();
        addAll.action = () -> {
            list.clear();
            searchingEnchants.clear();
            for (Enchantment e : Registries.ENCHANTMENT.stream().filter(Enchantment::isAvailableForEnchantedBookOffer).toList()) {
                searchingEnchants.add(new rollingEnchantment(Registries.ENCHANTMENT.getId(e), e.getMaxLevel(), getMinimumPrice(e, e.getMaxLevel()), false));
            }
            fillWidget(theme, list);
        };
        controls.row();

        WButton setOptimalForAll = controls.add(theme.button("Set optimal for all")).expandX().widget();
        setOptimalForAll.action = () -> {
            list.clear();
            for (rollingEnchantment e : searchingEnchants) {
                if (e.isVanilla()) {
                    Enchantment ench = Registries.ENCHANTMENT.get(e.enchantment);
                    e.maxCost = getMinimumPrice(ench, e.getMaxLevel());
                }
            }
            fillWidget(theme, list);
        };

        WButton priceBumpUp = controls.add(theme.button("+1 to price for all")).expandX().widget();
        priceBumpUp.action = () -> {
            list.clear();
            for (rollingEnchantment e : searchingEnchants) {
                if(e.maxCost < 64) {
                    e.maxCost++;
                }
            }
            fillWidget(theme, list);
        };

        WButton priceBumpDown = controls.add(theme.button("-1 to price for all")).expandX().widget();
        priceBumpDown.action = () -> {
            list.clear();
            for (rollingEnchantment e : searchingEnchants) {
                if(e.maxCost > 0) {
                    e.maxCost--;
                }
            }
            fillWidget(theme, list);
        };
        controls.row();

        WButton setZeroForAll = controls.add(theme.button("Set zero price for all")).expandX().widget();
        setZeroForAll.action = () -> {
            list.clear();
            for (rollingEnchantment e : searchingEnchants) {
                e.maxCost = 0;
            }
            fillWidget(theme, list);
        };

        WButton enableAll = controls.add(theme.button("Enable all")).expandX().widget();
        enableAll.action = () -> {
            list.clear();
            for (rollingEnchantment e : searchingEnchants) {
                e.enabled = true;
            }
            fillWidget(theme, list);
        };

        WButton disableAll = controls.add(theme.button("Disable all")).expandX().widget();
        disableAll.action = () -> {
            list.clear();
            for (rollingEnchantment e : searchingEnchants) {
                e.enabled = false;
            }
            fillWidget(theme, list);
        };
        controls.row();

    }

    public static int getMinimumPrice(Enchantment e, int l) {
//      TradeOffers.EnchantBookFactory.create()
//      Lnet/minecraft/village/TradeOffers$EnchantBookFactory;create(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/random/Random;)Lnet/minecraft/village/TradeOffer;
        if(e == null) {
            return 0;
        }
        return e.isTreasure() ? (2 + 3*l)*2 : 2 + 3*l;
    }

    public interface EnchantmentSelectCallback {
        void Selection(rollingEnchantment e);
    }

    public static class EnchantmentSelectScreen extends WindowScreen {
        private final List<Enchantment> available = Registries.ENCHANTMENT.stream().filter(Enchantment::isAvailableForEnchantedBookOffer).toList();
        private final GuiTheme theme;
        private final EnchantmentSelectCallback callback;
        private String filterText = "";

        public EnchantmentSelectScreen(GuiTheme theme1, EnchantmentSelectCallback callback1) {
            super(theme1, "Select enchantment");
            this.theme = theme1;
            this.callback = callback1;
        }

        @Override
        public void initWidgets() {
            WTable table = theme.table();
            table.minWidth = 400;

            WTextBox filter = add(theme.textBox(filterText)).minWidth(400).expandX().widget();
            filter.setFocused(true);
            filter.setCursorMax();
            filter.action = () -> {
                filterText = filter.get().trim();
                table.clear();
                fillTable(table);
            };
            filter.actionOnUnfocused = () -> {
                int n = 0;
                Enchantment ench = null;
                for (Enchantment e : available.stream().sorted((o1, o2) -> Names.get(o1).compareToIgnoreCase(Names.get(o2))).toList()) {
                    if (!filterText.equals("") && !Names.get(e).toLowerCase().startsWith(filterText.toLowerCase())) {
                        continue;
                    }
                    n++;
                    ench = e;
                    if(n == 2) {
                        return;
                    }
                }
                if(n == 1) {
                    callback.Selection(new rollingEnchantment(Registries.ENCHANTMENT.getId(ench), 0, 0, true));
                    close();
                }
            };

            WHorizontalList customList = add(theme.horizontalList()).expandX().widget();
            WTextBox cc = customList.add(theme.textBox("","Custom")).expandX().expandWidgetX().widget();
            WButton ca = customList.add(theme.button("Select")).widget();
            ca.action = () -> {
                callback.Selection(new rollingEnchantment(Identifier.tryParse(cc.get()), 0, 0, true));
                close();
            };
            cc.actionOnUnfocused = ca.action;

            add(table);
            fillTable(table);
        }

        private void fillTable(WTable table) {
            for (Enchantment e : available.stream().sorted((o1, o2) -> Names.get(o1).compareToIgnoreCase(Names.get(o2))).toList()) {
                if (!filterText.equals("") && !Names.get(e).toLowerCase().startsWith(filterText.toLowerCase())) {
                    continue;
                }
                table.add(theme.label(Names.get(e))).expandCellX();
                WButton a = table.add(theme.button("Select")).widget();
                a.action = () -> {
                    callback.Selection(new rollingEnchantment(Registries.ENCHANTMENT.getId(e), e.getMaxLevel(), getMinimumPrice(e, e.getMaxLevel()), true));
                    close();
                };
                table.row();
            }
        }
    }

    public void triggerInteract() {
        if (pauseOnScreen.get() && mc.currentScreen != null) {
            info("Rolling paused, interact with villager to continue");
        } else {
            assert mc.interactionManager != null;
            mc.interactionManager.interactEntity(mc.player, rollingVillager, Hand.MAIN_HAND);
        }
    }

    public Map<Identifier, Integer> getEnchants(ItemStack stack) {
        Map<Identifier, Integer> ret = Maps.newLinkedHashMap();
        if(!stack.isOf(Items.ENCHANTED_BOOK) || stack.getNbt() == null) {
            return ret;
        }
        NbtList list = stack.getNbt().getList("StoredEnchantments", NbtElement.COMPOUND_TYPE);
        for(int i = 0; i < list.size(); ++i) {
            NbtCompound c = list.getCompound(i);
            Identifier id = getIdFromNbt(c);
            if(id == null) {
                continue;
            }
            ret.put(id, getLevelFromNbt(c));
        }
        list = stack.getNbt().getList("Enchantments", NbtElement.COMPOUND_TYPE);
        for(int i = 0; i < list.size(); ++i) {
            NbtCompound c = list.getCompound(i);
            Identifier id = getIdFromNbt(c);
            if(id == null) {
                continue;
            }
            ret.put(id, getLevelFromNbt(c));
        }
        return ret;
    }

    public void triggerTradeCheck(TradeOfferList l) {
        if (currentState != State.RollingWaitingForVillagerTrades) {
            return;
        }
        for (TradeOffer offer : l) {
            // info(String.format("Offer: %s", offer.getSellItem().toString()));
            ItemStack sellItem = offer.getSellItem();

            Map<Identifier, Integer> sellingEnchants = getEnchants(sellItem);

            for (Map.Entry<Identifier, Integer> enchant : sellingEnchants.entrySet()) {
                int enchantLevel = enchant.getValue();
                Identifier enchantId = enchant.getKey();
                String enchantName = getEnchantmentName(enchantId);
                String enchantIdString = enchantId.toString();
                // level enchant.getValue()
                // enchantment enchant.getKey()
                boolean found = false;
                for (rollingEnchantment e : searchingEnchants) {
                    if (!e.enabled) {
                        continue;
                    }
                    if (!e.enchantment.toString().equals(enchantIdString)) {
                        continue;
                    }
                    found = true;
                    if (e.minLevel <= 0) {
                        int ml = e.getMaxLevel();
                        if (!e.isCustom() && enchantLevel != ml) {
                            info(String.format("Found enchant %s but it is not max level: %d (max) > %d (found)",
                                enchantName, ml, enchantLevel));
                            continue;
                        }
                    } else {
                        if (e.minLevel > enchantLevel) {
                            info(String.format("Found enchant %s but it has too low level: %d (requested level) > %d (rolled level)",
                                enchantName, e.minLevel, enchantLevel));
                            continue;
                        }
                    }
                    if (e.maxCost > 0 && offer.getOriginalFirstBuyItem().getCount() > e.maxCost) {
                        info(String.format("Found enchant %s but it costs too much: %s (max price) < %d (cost)",
                            enchantName, e.maxCost, offer.getOriginalFirstBuyItem().getCount()));
                        continue;
                    }
                    if (disableIfFound.get()) {
                        e.enabled = false;
                    }
                    toggle();
                    if (enablePlaySound.get() && sound.get().size() > 0) {
                        mc.getSoundManager().play(PositionedSoundInstance.master(this.sound.get().get(0),
                                soundPitch.get().floatValue(), soundVolume.get().floatValue()));
                    }
                    break;
                }
                if (!found) {
                    info(String.format("Found enchant %s but it is not in the list.", enchantName));
                }
            }
        }
        // ((MerchantScreenHandler)mc.player.currentScreenHandler).closeHandledScreen();
        assert mc.player != null;
        mc.player.closeHandledScreen();
        currentState = State.RollingBreakingBlock;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (currentState == State.WaitingForTargetBlock) {
            rollingBlockPos = event.blockPos;
            assert mc.world != null;
            rollingBlock = mc.world.getBlockState(rollingBlockPos).getBlock();
            currentState = State.WaitingForTargetVillager;
            info("Rolling block selected, now interact with villager you want to roll");
//            event.cancel(); //Dirty hack
        }
    }

    private void placeFailed(String msg) {
        if (failedToPlacePrevMsg + failedToPlaceDelay.get() <= System.currentTimeMillis()) {
            info(msg);
            failedToPlacePrevMsg = System.currentTimeMillis();
        }
        if (failedToPlaceDisable.get()) {
            toggle();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        assert mc.world != null;
        if (currentState == State.RollingBreakingBlock) {
            if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
                // info("Block is broken, waiting for villager to clean profession...");
                currentState = State.RollingWaitingForVillagerProfessionClear;
            } else {
                if (!BlockUtils.breakBlock(rollingBlockPos, true)) {
                    info("Can not break block");
                    toggle();
                }
            }
        } else if (currentState == State.RollingWaitingForVillagerProfessionClear) {
            if (mc.world.getBlockState(rollingBlockPos).isOf(Blocks.LECTERN)) {
                info("Rolling block mining reverted?");
                currentState = State.RollingBreakingBlock;
                return;
            }
            if (rollingVillager.getVillagerData().getProfession() == VillagerProfession.NONE) {
                // info("Profession cleared");
                currentState = State.RollingPlacingBlock;
            }
        } else if (currentState == State.RollingPlacingBlock) {
            FindItemResult item = InvUtils.findInHotbar(rollingBlock.asItem());
            if (!item.found()) {
                placeFailed("Lectern not found in hotbar");
                return;
            }
            if (!BlockUtils.canPlace(rollingBlockPos, true)) {
                placeFailed("Can't place lectern");
                return;
            }
            if (!BlockUtils.place(rollingBlockPos, item, headRotateOnPlace.get(), 5)) {
                placeFailed("Failed to place lectern");
                return;
            }
            currentState = State.RollingWaitingForVillagerProfessionNew;
        } else if (currentState == State.RollingWaitingForVillagerProfessionNew) {
            if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
                info("Lectern placement reverted by server (AC?)");
                currentState = State.RollingPlacingBlock;
                return;
            }
            if (!mc.world.getBlockState(rollingBlockPos).isOf(Blocks.LECTERN)) {
                info("Placed wrong block?!");
                currentState = State.RollingBreakingBlock;
                return;
            }
            if (rollingVillager.getVillagerData().getProfession() != VillagerProfession.NONE) {
                currentState = State.RollingWaitingForVillagerTrades;
                triggerInteract();
            }
        }
    }

    public String getEnchantmentName(Identifier id) {
        String ret = id.toString();
        var e = Registries.ENCHANTMENT.getOrEmpty(id);
        if (e.isPresent()) {
            ret = Names.get(e.get());
        }
        return ret;
    }

    public static class rollingEnchantment implements ISerializable<rollingEnchantment> {
        public Identifier enchantment;
        public int minLevel;
        public int maxCost;
        public boolean enabled;

        public rollingEnchantment(Identifier _enchantment, int _minLevel, int _maxCost, boolean _enabled) {
            this.enchantment = _enchantment;
            this.minLevel = _minLevel;
            this.maxCost = _maxCost;
            this.enabled = _enabled;
        }

        public rollingEnchantment() {
            this.enchantment = Registries.ENCHANTMENT.getId(Enchantments.PROTECTION);
            this.minLevel = 0;
            this.maxCost = 0;
            this.enabled = false;
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putString("enchantment", this.enchantment.toString());
            tag.putInt("minLevel", this.minLevel);
            tag.putInt("maxCost", this.maxCost);
            tag.putBoolean("enabled", this.enabled);
            return tag;
        }

        @Override
        public rollingEnchantment fromTag(NbtCompound tag) {
            this.enchantment = Identifier.tryParse(tag.getString("enchantment"));
            this.minLevel = tag.getInt("minLevel");
            this.maxCost = tag.getInt("maxCost");
            this.enabled = tag.getBoolean("enabled");
            return this;
        }

        public boolean isCustom() {
            return !Registries.ENCHANTMENT.containsId(this.enchantment);
        }
        public boolean isVanilla() {
            return Registries.ENCHANTMENT.containsId(this.enchantment);
        }
        public int getMaxLevel() {
            int maxlevel = 0;
            var e = Registries.ENCHANTMENT.getOrEmpty(this.enchantment);
            if (e.isPresent()) {
                maxlevel = e.get().getMaxLevel();
            }
            return maxlevel;
        }
        public String getName() {
            String ret = this.enchantment.toString();
            var e = Registries.ENCHANTMENT.getOrEmpty(this.enchantment);
            if (e.isPresent()) {
                ret = Names.get(e.get());
            }
            return ret;
        }
    }
}
