package maxsuperman.addons.roller;

import maxsuperman.addons.roller.modules.VillagerRoller;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VillagerRollerAddon extends MeteorAddon {
    public static final Logger LOG = LogManager.getLogger();

    @Override
    public void onInitialize() {
        LOG.info("Initializing Meteor Villager Roller");

        // Modules
        Modules.get().add(new VillagerRoller());
    }

    @Override
    public void onRegisterCategories() {
        // Custom category not defined
    }

    @Override
    public String getPackage() {
        return "maxsuperman.addons.roller";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("maxsupermanhd", "meteor-villager-roller");
    }
}
