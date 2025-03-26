package maxsuperman.addons.roller.modules;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import maxsuperman.addons.roller.gui.screens.EnchantmentSelectScreen;
import maxsuperman.addons.roller.utils.BlockPlacerFactory;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
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
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class VillagerRoller extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSound = settings.createGroup("Sound");
    private final SettingGroup sgChatFeedback = settings.createGroup("Chat feedback", false);

    private final Setting<Boolean> disableIfFound = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-when-found")
        .description("Disable enchantment from list if found")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> disconnectIfFound = sgGeneral.add(new BoolSetting.Builder()
        .name("disconnect-when-found")
        .description("Disconnect from server when enchantment from list if found")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> saveListToConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("save-list-to-config")
        .description("Toggles saving and loading of rolling list to config and copypaste buffer")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> enablePlaySound = sgGeneral.add(new BoolSetting.Builder()
        .name("enable-sound")
        .description("Plays sound when it finds desired trade")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<SoundEvent>> sound = sgSound.add(new SoundEventListSetting.Builder()
        .name("sound-to-play")
        .description("Sound that will be played when desired trade is found if enabled")
        .defaultValue(Collections.singletonList(SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK))
        .build()
    );

    private final Setting<Double> soundPitch = sgSound.add(new DoubleSetting.Builder()
        .name("sound-pitch")
        .description("Playing sound pitch")
        .defaultValue(1.0)
        .min(0)
        .sliderRange(0, 8)
        .build()
    );

    private final Setting<Double> soundVolume = sgSound.add(new DoubleSetting.Builder()
        .name("sound-volume")
        .description("Playing sound volume")
        .defaultValue(1.0)
        .min(0)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<Boolean> pauseOnScreen = sgGeneral.add(new BoolSetting.Builder()
        .name("pause-on-screens")
        .description("Pauses rolling if any screen is open")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> headRotateOnPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate-place")
        .description("Look to the block while placing it?")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("Utilize Baritone for breaking and placing the lectern. Enable this if normal block breaking or placement is being reverted by the server's anticheat.")
        .visible(() -> BaritoneUtils.IS_AVAILABLE)
        .defaultValue(false)
        .build());

    private final Setting<Integer> baritoneBlockActionTimeout = sgGeneral.add(new IntSetting.Builder()
        .name("baritone-action-timeout")
        .description("Delay after failed baritone block place/break (milliseconds)")
        .visible(useBaritone::get)
        .defaultValue(1500)
        .min(0)
        .sliderRange(0, 10000)
        .build()
    );

    private final Setting<Boolean> lookAtVillagerInteract = sgGeneral.add(new BoolSetting.Builder()
        .name("look-at-villager")
        .description("Look at the villager before interacting with it (required on some servers)")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> failedToPlaceDelay = sgGeneral.add(new IntSetting.Builder()
        .name("place-fail-delay")
        .description("Delay after failed block place (milliseconds)")
        .defaultValue(1500)
        .min(0)
        .sliderRange(0, 10000)
        .build()
    );

    private final Setting<Boolean> failedToPlaceDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("place-fail-disable")
        .description("Disables roller if block placement fails")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maxProfessionWaitTime = sgGeneral.add(new IntSetting.Builder()
        .name("max-profession-wait-time")
        .description("Time to wait if villager does not take profession (milliseconds). Zero = unlimited.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 10000)
        .build()
    );

    private final Setting<Integer> maxInteractWaitTime = sgGeneral.add(new IntSetting.Builder()
        .name("max-interact-wait-time")
        .description("Delay after failed villager interact (milliseconds)")
        .defaultValue(500)
        .min(0)
        .sliderRange(0, 10000)
        .build()
    );

    private final Setting<Boolean> onlyTradable = sgGeneral.add(new BoolSetting.Builder()
        .name("only-tradable")
        .description("Hide enchantments that are not marked as tradable")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> sortEnchantments = sgGeneral.add(new BoolSetting.Builder()
        .name("sort-enchantments")
        .description("Show enchantments sorted by their name")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfSetup = sgChatFeedback.add(new BoolSetting.Builder()
        .name("setup")
        .description("Hints on what to do in the beginning (otherwise denoted in modules list state)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfInteractTimeout = sgChatFeedback.add(new BoolSetting.Builder()
        .name("interact-timeout")
        .description("Villager interact packet timeout")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfPausedOnScreen = sgChatFeedback.add(new BoolSetting.Builder()
        .name("paused-on-screen")
        .description("Rolling paused, interact with villager to continue")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instantRebreak = sgGeneral.add(new BoolSetting.Builder()
        .name("CivBreak")
        .description("Uses CivBreak to mine the lecturn instantly. Best to just stay over the lecturn slot.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> cfLowerLevel = sgChatFeedback.add(new BoolSetting.Builder()
        .name("found-lower-level")
        .description("Found enchant %s but it is not max level: %d (max) > %d (found)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfTooExpensive = sgChatFeedback.add(new BoolSetting.Builder()
        .name("found-too-expensive")
        .description("Found enchant %s but it costs too much: %s (max price) < %d (cost)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfIgnored = sgChatFeedback.add(new BoolSetting.Builder()
        .name("found-not-on-the-list")
        .description("Found enchant %s but it is not in the list.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfProfessionTimeout = sgChatFeedback.add(new BoolSetting.Builder()
        .name("profession-timeout")
        .description("Villager did not take profession within the specified time")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfPlaceFailed = sgChatFeedback.add(new BoolSetting.Builder()
        .name("place-failed")
        .description("Failed placing, can't place or can't get lectern to hotbar (they still trigger place-failed settings)")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> cfDiscrepancy = sgChatFeedback.add(new BoolSetting.Builder()
        .name("discrepancy")
        .description("Somehow roller got into state it was not expecting (likely AC mess)")
        .defaultValue(true)
        .build()
    );


    private enum State {
        DISABLED,
        WAITING_FOR_TARGET_BLOCK,
        WAITING_FOR_TARGET_VILLAGER,
        ROLLING_BREAKING_BLOCK,
        ROLLING_WAITING_FOR_BARITONE_BLOCK_BREAK,
        ROLLING_WAITING_FOR_VILLAGER_PROFESSION_CLEAR,
        ROLLING_PLACING_BLOCK,
        ROLLING_WAITING_FOR_BARITONE_BLOCK_PLACE,
        ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW,
        ROLLING_WAITING_FOR_VILLAGER_TRADES
    }

    private static final Path CONFIG_PATH = MeteorClient.FOLDER.toPath().resolve("VillagerRoller");
    private State currentState = State.DISABLED;
    private VillagerEntity rollingVillager;
    private BlockPos rollingBlockPos;
    private Block rollingBlock;
    private final List<RollingEnchantment> searchingEnchants = new ArrayList<>();
    private long failedToPlacePrevMsg = System.currentTimeMillis();
    private long prevVillagerInteractTime = System.currentTimeMillis();
    private long prevBaritoneBlockBreak = System.currentTimeMillis();
    private long prevBaritoneBlockPlace = System.currentTimeMillis();
    private long currentProfessionWaitTime;

    public VillagerRoller() {
        super(Categories.Misc, "villager-roller", "Rolls trades.");
    }

    @Override
    public void onActivate() {
        if (toggleOnBindRelease) {
            toggleOnBindRelease = false;
            if (cfSetup.get()) {
                warning("You had 'Toggle on bind release' set to true, I just saved you some troubleshooting by turning it off");
            }
        }
        currentState = State.WAITING_FOR_TARGET_BLOCK;
        if (cfSetup.get()) {
            info("Attack block you want to roll");
        }
    }

    @Override
    public void onDeactivate() {
        currentState = State.DISABLED;
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
            for (RollingEnchantment e : searchingEnchants) {
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
                searchingEnchants.add(new RollingEnchantment().fromTag((NbtCompound) e));
            }
        }
        return this;
    }

    private boolean loadSearchingFromFile(File f) {
        if (!f.exists() || !f.canRead()) {
            error("File does not exist or can not be loaded");
            return false;
        }
        NbtCompound r = null;
        try {
            r = NbtIo.read(f.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (r == null) {
            error("Failed to load nbt from file");
            return false;
        }
        NbtList l = r.getList("rolling", NbtElement.COMPOUND_TYPE);
        searchingEnchants.clear();
        for (NbtElement e : l) {
            if (e.getType() != NbtElement.COMPOUND_TYPE) {
                error("Invalid list element");
                return false;
            }
            searchingEnchants.add(new RollingEnchantment().fromTag((NbtCompound) e));
        }
        return true;
    }

    public boolean saveSearchingToFile(File f) {
        NbtList l = new NbtList();
        for (RollingEnchantment e : searchingEnchants) {
            l.add(e.toTag());
        }
        NbtCompound c = new NbtCompound();
        c.put("rolling", l);
        if (Files.notExists(f.getParentFile().toPath()) && !f.getParentFile().mkdirs()) {
            error("Failed to make directories");
            return false;
        }
        try {
            NbtIo.write(c, f.toPath());
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

        WTextBox savedConfigName = control.add(theme.textBox("default")).expandWidgetX().expandCellX().expandX().widget();
        WButton save = control.add(theme.button("Save")).expandX().widget();
        save.action = () -> {
            if (saveSearchingToFile(new File(new File(MeteorClient.FOLDER, "VillagerRoller"), savedConfigName.get() + ".nbt"))) {
                info("Saved successfully");
            } else {
                error("Save failed");
            }
            list.clear();
            fillWidget(theme, list);
        };
        control.row();

        ArrayList<String> configs = new ArrayList<>();
        if (Files.notExists(CONFIG_PATH)) {
            if (!CONFIG_PATH.toFile().mkdirs()) error("Failed to create directory [{}]", CONFIG_PATH);
        } else {
            try (DirectoryStream<Path> configDir = Files.newDirectoryStream(CONFIG_PATH)) {
                for (Path config : configDir) {
                    configs.add(FilenameUtils.removeExtension(config.getFileName().toString()));
                }
            } catch (IOException e) {
                error("Failed to list directory", e);
            }
        }
        if (!configs.isEmpty()) {
            WDropdown<String> loadedConfigName = control.add(theme.dropdown(configs.toArray(new String[0]), "default")).expandWidgetX().expandCellX().expandX().widget();
            WButton load = control.add(theme.button("Load")).expandX().widget();
            load.action = () -> {
                if (loadSearchingFromFile(new File(new File(MeteorClient.FOLDER, "VillagerRoller"), loadedConfigName.get() + ".nbt"))) {
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
            searchingEnchants.removeIf(ench -> ench.enchantment == null);
            searchingEnchants.sort(Comparator.comparing(o -> o.enchantment));
        }

        Optional<Registry<Enchantment>> reg;
        if (mc.world != null) {
            reg = mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        } else {
            reg = Optional.empty();
        }

        for (int i = 0; i < searchingEnchants.size(); i++) {
            RollingEnchantment e = searchingEnchants.get(i);
            Optional<RegistryEntry.Reference<Enchantment>> en;
            if (reg.isPresent()) {
                en = reg.get().getEntry(e.enchantment);
            } else {
                en = Optional.empty();
            }
            final int si = i;
            ItemStack book = Items.ENCHANTED_BOOK.getDefaultStack();
            int maxlevel = 255;
            if (en.isPresent()) {
                book = EnchantmentHelper.getEnchantedBookWith(new EnchantmentLevelEntry(en.get(), en.get().value().getMaxLevel()));
                maxlevel = en.get().value().getMaxLevel();
            }
            table.add(theme.item(book));

            WHorizontalList label = theme.horizontalList();
            WButton c = label.add(theme.button("Change")).widget();
            c.action = () -> mc.setScreen(new EnchantmentSelectScreen(theme, onlyTradable.get(), sel -> {
                searchingEnchants.set(si, sel);
                list.clear();
                fillWidget(theme, list);
            }));
            if (en.isPresent()) {
                label.add(theme.label(Names.get(en.get())));
            } else {
                label.add(theme.label(e.enchantment.toString()));
            }
            table.add(label);

            WIntEdit lev = table.add(theme.intEdit(e.minLevel, 0, maxlevel, true)).minWidth(40).expandX().widget();
            lev.action = () -> e.minLevel = lev.get();
            lev.tooltip = "Minimum enchantment level, 0 acts as maximum possible only (for custom 0 acts like 1)";

            WHorizontalList costbox = table.add(theme.horizontalList()).minWidth(50).expandX().widget();
            WIntEdit cost = costbox.add(theme.intEdit(e.maxCost, 0, 64, false)).minWidth(40).expandX().widget();
            cost.action = () -> e.maxCost = cost.get();
            cost.tooltip = "Maximum cost in emeralds, 0 means no limit";

            WButton setOptimal = costbox.add(theme.button("O")).widget();
            setOptimal.tooltip = "Set to optimal price (2 + maxLevel*3) (double if treasure) (if known)";
            setOptimal.action = () -> {
                list.clear();
                en.ifPresent(enchantmentReference -> e.maxCost = getMinimumPrice(enchantmentReference));
                fillWidget(theme, list);
            };

            WCheckbox enabled = table.add(theme.checkbox(e.enabled)).widget();
            enabled.action = () -> e.enabled = enabled.checked;
            enabled.tooltip = "Enabled?";

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

        WButton add = controls.add(theme.button("Add")).expandX().widget();
        add.action = () -> mc.setScreen(new EnchantmentSelectScreen(theme, onlyTradable.get(), e -> {
            e.minLevel = 1;
            e.maxCost = 64;
            e.enabled = true;
            searchingEnchants.add(e);
            list.clear();
            fillWidget(theme, list);
        }));

        WButton addAll = controls.add(theme.button("Add all")).expandX().widget();
        addAll.action = () -> {
            list.clear();
            searchingEnchants.clear();
            if (reg.isPresent()) {
                for (RegistryEntry<Enchantment> e : getEnchants(onlyTradable.get())) {
                    searchingEnchants.add(new RollingEnchantment(reg.get().getId(e.value()), e.value().getMaxLevel(), getMinimumPrice(e), true));
                }
            }
            fillWidget(theme, list);
        };
        controls.row();

        WButton setOptimalForAll = controls.add(theme.button("Set optimal for all")).expandX().widget();
        setOptimalForAll.action = () -> {
            list.clear();
            if (reg.isPresent()) {
                for (RollingEnchantment e : searchingEnchants) {
                    reg.get().getEntry(e.enchantment).ifPresent(enchantmentReference -> e.maxCost = getMinimumPrice(enchantmentReference));
                }
            }
            fillWidget(theme, list);
        };

        WButton priceBumpUp = controls.add(theme.button("+1 to price for all")).expandX().widget();
        priceBumpUp.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                if (e.maxCost < 64) e.maxCost++;
            }
            fillWidget(theme, list);
        };

        WButton priceBumpDown = controls.add(theme.button("-1 to price for all")).expandX().widget();
        priceBumpDown.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                if (e.maxCost > 0) e.maxCost--;
            }
            fillWidget(theme, list);
        };
        controls.row();

        WButton setZeroForAll = controls.add(theme.button("Set zero price for all")).expandX().widget();
        setZeroForAll.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                e.maxCost = 0;
            }
            fillWidget(theme, list);
        };

        WButton enableAll = controls.add(theme.button("Enable all")).expandX().widget();
        enableAll.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                e.enabled = true;
            }
            fillWidget(theme, list);
        };

        WButton disableAll = controls.add(theme.button("Disable all")).expandX().widget();
        disableAll.action = () -> {
            list.clear();
            for (RollingEnchantment e : searchingEnchants) {
                e.enabled = false;
            }
            fillWidget(theme, list);
        };
        controls.row();

    }

    public List<RegistryEntry<Enchantment>> getEnchants(boolean onlyTradable) {
        if (mc.world == null) {
            return Collections.emptyList();
        }
        var reg = mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if (reg.isEmpty()) {
            return Collections.emptyList();
        }
        List<RegistryEntry<Enchantment>> available = new ArrayList<>();
        if (onlyTradable) {
            var i = reg.get().iterateEntries(EnchantmentTags.TRADEABLE);
            i.iterator().forEachRemaining(available::add);
            return available;
        } else {
            for (var a : reg.get().getIndexedEntries()) {
                available.add(a);
            }
            return available;
        }
    }

    public static int getMinimumPrice(RegistryEntry<Enchantment> e) {
        if (e == null) return 0;
        return e.isIn(EnchantmentTags.DOUBLE_TRADE_PRICE) ? (2 + 3 * e.value().getMaxLevel()) * 2 : 2 + 3 * e.value().getMaxLevel();
    }

    public void lookAtVillager(Vec3d playerPos, Vec3d villagerPos) {
        Vec3d direction = villagerPos.subtract(playerPos).normalize();
        double yaw = Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        double pitch = Math.toDegrees(-Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));

        // Add some random noise to prevent anticheat detection
        yaw += (Math.random() - 0.5) * 2;
        pitch += (Math.random() - 0.5) * 2;

        BlockPlacerFactory.getBlockPlacer(useBaritone.get()).lookAt((float) yaw, (float) pitch);
    }

    public void triggerInteract() {
        if (pauseOnScreen.get() && mc.currentScreen != null) {
            if (cfPausedOnScreen.get()) {
                info("Rolling paused, interact with villager to continue");
            }
        } else {
            Vec3d playerPos = mc.player.getEyePos();
            Vec3d villagerPos = rollingVillager.getEyePos();
            EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, playerPos, villagerPos, rollingVillager.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(villagerPos));
            if (entityHitResult == null) {
                // Raycast didn't find villager entity?
                ActionResult actionResultDirect = mc.interactionManager.interactEntity(mc.player, rollingVillager, Hand.MAIN_HAND);
                if (!actionResultDirect.isAccepted()) {
                    currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW;
                }
            } else {
                if (lookAtVillagerInteract.get()) {
                    lookAtVillager(playerPos, villagerPos);
                }

                ActionResult actionResult = mc.interactionManager.interactEntityAtLocation(mc.player, rollingVillager, entityHitResult, Hand.MAIN_HAND);
                if (!actionResult.isAccepted()) {
                    ActionResult actionResultDirect = mc.interactionManager.interactEntity(mc.player, rollingVillager, Hand.MAIN_HAND);
                    if (!actionResultDirect.isAccepted()) {
                        currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW;
                    }
                }
            }
            prevVillagerInteractTime = System.currentTimeMillis();
        }
    }

    public List<Pair<RegistryEntry<Enchantment>, Integer>> getEnchants(ItemStack stack) {
        List<Pair<RegistryEntry<Enchantment>, Integer>> ret = new ArrayList<>();
        for (var e : EnchantmentHelper.getEnchantments(stack).getEnchantmentEntries()) {
            ret.add(ObjectIntImmutablePair.of(e.getKey(), e.getIntValue()));
        }
        return ret;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (currentState != State.ROLLING_WAITING_FOR_VILLAGER_TRADES) return;
        if (!(event.packet instanceof SetTradeOffersS2CPacket p)) return;
        mc.executeSync(() -> triggerTradeCheck(p.getOffers()));
    }

    public void triggerTradeCheck(TradeOfferList l) {
        for (TradeOffer offer : l) {
            ItemStack sellItem = offer.getSellItem();
            if (!sellItem.isOf(Items.ENCHANTED_BOOK) || sellItem.get(DataComponentTypes.STORED_ENCHANTMENTS) == null)
                break;

            for (Pair<RegistryEntry<Enchantment>, Integer> enchant : getEnchants(sellItem)) {
                int enchantLevel = enchant.right();
                var reg = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                String enchantIdString = reg.getId(enchant.key().value()).toString();
                String enchantName = Names.get(enchant.key());

                boolean found = false;
                for (RollingEnchantment e : searchingEnchants) {
                    if (!e.enabled || !e.enchantment.toString().equals(enchantIdString)) continue;
                    found = true;
                    if (e.minLevel <= 0) {
                        int ml = enchant.key().value().getMaxLevel();
                        if (enchantLevel < ml) {
                            if (cfLowerLevel.get()) {
                                info(String.format("Found enchant %s but it is not max level: %d (max) > %d (found)",
                                    enchantName, ml, enchantLevel));
                            }
                            continue;
                        }
                    } else if (e.minLevel > enchantLevel) {
                        if (cfLowerLevel.get()) {
                            info(String.format("Found enchant %s but it has too low level: %d (requested level) > %d (rolled level)",
                                enchantName, e.minLevel, enchantLevel));
                        }
                        continue;
                    }
                    if (e.maxCost > 0 && offer.getOriginalFirstBuyItem().getCount() > e.maxCost) {
                        if (cfTooExpensive.get()) {
                            info(String.format("Found enchant %s but it costs too much: %s (max price) < %d (cost)",
                                enchantName, e.maxCost, offer.getOriginalFirstBuyItem().getCount()));
                        }
                        continue;
                    }
                    if (disableIfFound.get()) e.enabled = false;
                    toggle();
                    if (enablePlaySound.get() && !sound.get().isEmpty()) {
                        mc.getSoundManager().play(PositionedSoundInstance.master(sound.get().get(0),
                            soundPitch.get().floatValue(), soundVolume.get().floatValue()));
                    }
                    if (disconnectIfFound.get()) {
                        MutableText text = Text.literal(String.format("[VillagerRoller] Found enchant %s for %d emeralds and automatically disconnected.", enchantName, offer.getOriginalFirstBuyItem().getCount()));
                        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(text));
                    }
                    break;
                }
                if (!found && cfIgnored.get()) {
                    info(String.format("Found enchant %s but it is not in the list.", enchantName));
                }
            }
        }

        mc.player.closeHandledScreen();
        currentState = State.ROLLING_BREAKING_BLOCK;
    }

    @EventHandler
    private void onInteractEntity(InteractEntityEvent event) {
        if (currentState != State.WAITING_FOR_TARGET_VILLAGER) return;
        if (!(event.entity instanceof VillagerEntity villager)) return;

        rollingVillager = villager;
        currentState = State.ROLLING_BREAKING_BLOCK;
        if (cfSetup.get()) {
            info("We got your villager");
        }
        event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onStartBreakingBlockEvent(StartBreakingBlockEvent event) {
        if (currentState != State.WAITING_FOR_TARGET_BLOCK) return;

        rollingBlockPos = event.blockPos;
        rollingBlock = mc.world.getBlockState(rollingBlockPos).getBlock();
        currentState = State.WAITING_FOR_TARGET_VILLAGER;
        if (cfSetup.get()) {
            if (instantRebreak.get()) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, rollingBlockPos, Direction.UP));
            }
            info("Rolling block selected, now interact with villager you want to roll");
        }
    }

    private void placeFailed(String msg) {
        if (failedToPlacePrevMsg + failedToPlaceDelay.get() <= System.currentTimeMillis()) {
            if (cfPlaceFailed.get()) {
                info(msg);
            }
            failedToPlacePrevMsg = System.currentTimeMillis();
        }
        if (failedToPlaceDisable.get()) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        switch (currentState) {
            case ROLLING_BREAKING_BLOCK -> {
                if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
                    // info("Block is broken, waiting for villager to clean profession...");
                    currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_CLEAR;
                    return;
                }
                if (instantRebreak.get()) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,rollingBlockPos, Direction.DOWN));
                }

                if (!instantRebreak.get() && !BlockPlacerFactory.getBlockPlacer(useBaritone.get()).breakBlock(rollingBlockPos)) {
                    error("Can not break specified block");
                    toggle();
                }
                if (useBaritone.get()) {
                    prevBaritoneBlockBreak = System.currentTimeMillis();
                    currentState = State.ROLLING_WAITING_FOR_BARITONE_BLOCK_BREAK;
                }
            }
            case ROLLING_WAITING_FOR_BARITONE_BLOCK_BREAK -> {
                if (baritoneBlockActionTimeout.get() + prevBaritoneBlockBreak <= System.currentTimeMillis()) {
                    if (cfPlaceFailed.get()) {
                        info("Baritone failed to break block");
                    }
                    currentState = State.ROLLING_BREAKING_BLOCK;
                    return;
                }

                if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
                    currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_CLEAR;
                }
            }
            case ROLLING_WAITING_FOR_VILLAGER_PROFESSION_CLEAR -> {
                if (mc.world.getBlockState(rollingBlockPos).isOf(Blocks.LECTERN)) {
                    if (cfDiscrepancy.get()) {
                        info("Rolling block mining reverted?");
                    }
                    currentState = State.ROLLING_BREAKING_BLOCK;
                    return;
                }
                if (rollingVillager.getVillagerData().getProfession() == VillagerProfession.NONE) {
                    // info("Profession cleared");
                    currentState = State.ROLLING_PLACING_BLOCK;
                }
            }
            case ROLLING_PLACING_BLOCK -> {
                // Check if failedToPlaceDelay has passed
                if (failedToPlacePrevMsg + failedToPlaceDelay.get() > System.currentTimeMillis()) {
                    return;
                }

                if (!BlockPlacerFactory.getBlockPlacer(useBaritone.get()).placeBlock(rollingBlockPos, headRotateOnPlace.get())) {
                    placeFailed("Failed to place lectern.  Lectern missing or can't be placed");
                    return;
                }
                if (useBaritone.get()) {
                    prevBaritoneBlockPlace = System.currentTimeMillis();
                    currentState = State.ROLLING_WAITING_FOR_BARITONE_BLOCK_PLACE;
                    return;
                }

                currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW;
                if (maxProfessionWaitTime.get() > 0) {
                    currentProfessionWaitTime = System.currentTimeMillis();
                }
            }
            case ROLLING_WAITING_FOR_BARITONE_BLOCK_PLACE -> {
                if (baritoneBlockActionTimeout.get() + prevBaritoneBlockPlace <= System.currentTimeMillis()) {
                    if (cfPlaceFailed.get()) {
                        info("Baritone failed to place lectern");
                    }
                    currentState = State.ROLLING_BREAKING_BLOCK;
                    return;
                }

                if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
                    return;
                }

                currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW;
                if (maxProfessionWaitTime.get() > 0) {
                    currentProfessionWaitTime = System.currentTimeMillis();
                }
            }
            case ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW -> {
                if (maxProfessionWaitTime.get() > 0 && (currentProfessionWaitTime + maxProfessionWaitTime.get() <= System.currentTimeMillis())) {
                    if (cfProfessionTimeout.get()) {
                        info("Villager did not take profession within the specified time");
                    }
                    currentState = State.ROLLING_BREAKING_BLOCK;
                    return;
                }
                if (mc.world.getBlockState(rollingBlockPos) == Blocks.AIR.getDefaultState()) {
                    if (cfDiscrepancy.get()) {
                        info("Lectern placement reverted by server (AC?)");
                    }
                    currentState = State.ROLLING_PLACING_BLOCK;
                    return;
                }
                if (!mc.world.getBlockState(rollingBlockPos).isOf(Blocks.LECTERN)) {
                    if (cfDiscrepancy.get()) {
                        info("Placed wrong block?!");
                    }
                    currentState = State.ROLLING_BREAKING_BLOCK;
                    return;
                }
                if (rollingVillager.getVillagerData().getProfession() != VillagerProfession.NONE) {
                    currentState = State.ROLLING_WAITING_FOR_VILLAGER_TRADES;
                    triggerInteract();
                }
            }
            case ROLLING_WAITING_FOR_VILLAGER_TRADES -> {
                if (prevVillagerInteractTime + maxInteractWaitTime.get() <= System.currentTimeMillis()) {
                    if (cfInteractTimeout.get()) {
                        info("Villager interact packet timeout");
                        prevVillagerInteractTime = 0;
                    }
                    // We want to retry interact
                    currentState = State.ROLLING_WAITING_FOR_VILLAGER_PROFESSION_NEW;
                }
            }
            default -> {
                // Wait for another state
            }
        }
    }

    public static class RollingEnchantment implements ISerializable<RollingEnchantment> {
        private Identifier enchantment;
        private int minLevel;
        private int maxCost;
        private boolean enabled;

        public RollingEnchantment(Identifier enchantment, int minLevel, int maxCost, boolean enabled) {
            this.enchantment = enchantment;
            this.minLevel = minLevel;
            this.maxCost = maxCost;
            this.enabled = enabled;
        }

        public RollingEnchantment() {
            enchantment = Identifier.of("minecraft", "protection");
            minLevel = 0;
            maxCost = 0;
            enabled = false;
        }

        @Override
        public NbtCompound toTag() {
            NbtCompound tag = new NbtCompound();
            tag.putString("enchantment", enchantment.toString());
            tag.putInt("minLevel", minLevel);
            tag.putInt("maxCost", maxCost);
            tag.putBoolean("enabled", enabled);
            return tag;
        }

        @Override
        public RollingEnchantment fromTag(NbtCompound tag) {
            enchantment = Identifier.tryParse(tag.getString("enchantment"));
            minLevel = tag.getInt("minLevel");
            maxCost = tag.getInt("maxCost");
            enabled = tag.getBoolean("enabled");
            return this;
        }
    }
}
