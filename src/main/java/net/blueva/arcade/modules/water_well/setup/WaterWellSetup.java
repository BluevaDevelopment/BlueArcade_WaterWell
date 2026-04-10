package net.blueva.arcade.modules.water_well.setup;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
import net.blueva.arcade.api.setup.TabCompleteContext;
import net.blueva.arcade.api.setup.TabCompleteResult;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class WaterWellSetup implements GameSetupHandler {

    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;

    public WaterWellSetup(ModuleConfigAPI moduleConfig, CoreConfigAPI coreConfig) {
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
    }

    @Override
    public boolean handle(SetupContext context) {
        // No module-specific subcommands needed
        return false;
    }

    @Override
    public TabCompleteResult tabComplete(TabCompleteContext context) {
        return TabCompleteResult.empty();
    }

    @Override
    public List<String> getSubcommands() {
        return List.of();
    }

    @Override
    public boolean validateConfig(SetupContext context) {
        // No special validation needed — bounds and spawns are handled by core
        return true;
    }
}
