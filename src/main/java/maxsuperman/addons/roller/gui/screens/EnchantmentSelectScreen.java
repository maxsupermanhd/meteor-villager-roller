package maxsuperman.addons.roller.gui.screens;

import maxsuperman.addons.roller.modules.VillagerRoller;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EnchantmentSelectScreen extends WindowScreen {
    private final GuiTheme theme;
    private final EnchantmentSelectCallback callback;
    private String filterText = "";
    private final boolean onlyTradable;

    public EnchantmentSelectScreen(GuiTheme theme, boolean onlyTradable, EnchantmentSelectCallback callback) {
        super(theme, "Select enchantment");
        this.theme = theme;
        this.callback = callback;
        this.onlyTradable = onlyTradable;
    }

    public interface EnchantmentSelectCallback {
        void selection(VillagerRoller.RollingEnchantment e);
    }

    @Override
    public void initWidgets() {
        WTable table = theme.table();
        table.minWidth = 400;

        WTextBox filter = add(theme.textBox(filterText, "Search")).minWidth(400).expandX().widget();
        filter.setFocused(true);
        filter.setCursorMax();
        filter.action = () -> {
            filterText = filter.get().trim();
            table.clear();
            fillTable(table);
        };

        WHorizontalList customList = add(theme.horizontalList()).expandX().widget();
        WTextBox cc = customList.add(theme.textBox("", "Custom")).expandX().expandWidgetX().widget();
        WButton ca = customList.add(theme.button("Select")).widget();
        ca.action = () -> {
            String idtext = cc.get();
            if (idtext.isEmpty()) return;
            Identifier id = Identifier.tryParse(cc.get());
            if (id == null) return;
            callback.selection(new VillagerRoller.RollingEnchantment(id, 0, 0, true));
            close();
        };

        add(table);
        fillTable(table);
    }

    private void fillTable(WTable table) {
        if (mc.world == null) {
            return;
        }
        var reg = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        List<RegistryEntry<Enchantment>> available = new ArrayList<>();
        if (this.onlyTradable) {
            var l = reg.getEntryList(EnchantmentTags.TRADEABLE);
            if (l.isEmpty()) {
                return;
            }
            available = l.get().stream().toList();
        } else {
            for (var a : reg.getIndexedEntries()) {
                available.add(a);
            }
        }
        for (RegistryEntry<Enchantment> e : available.stream().sorted((o1, o2) -> Names.get(o1).compareToIgnoreCase(Names.get(o2))).toList()) {
            if (!filterText.isEmpty() && !Names.get(e).toLowerCase().startsWith(filterText.toLowerCase())) {
                continue;
            }
            table.add(theme.label(Names.get(e))).expandCellX();
            WButton a = table.add(theme.button("Select")).widget();
            a.action = () -> {
                callback.selection(new VillagerRoller.RollingEnchantment(reg.getId(e.value()), e.value().getMaxLevel(), VillagerRoller.getMinimumPrice(e), true));
                close();
            };
            table.row();
        }
    }
}
