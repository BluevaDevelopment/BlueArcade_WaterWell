package net.blueva.arcade.modules.water_well.support.stats;

import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatDefinition;
import net.blueva.arcade.api.stats.StatScope;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import org.bukkit.entity.Player;

import java.util.Collection;

public class WaterWellStatsService {

    private final StatsAPI statsAPI;
    private final ModuleInfo moduleInfo;
    private final ModuleConfigAPI moduleConfig;

    public WaterWellStatsService(StatsAPI statsAPI, ModuleInfo moduleInfo, ModuleConfigAPI moduleConfig) {
        this.statsAPI = statsAPI;
        this.moduleInfo = moduleInfo;
        this.moduleConfig = moduleConfig;
    }

    public void registerStats() {
        if (statsAPI == null) {
            return;
        }

        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("wins", moduleConfig.getStringFrom("language.yml", "stats.labels.wins", "Wins"), moduleConfig.getStringFrom("language.yml", "stats.descriptions.wins", "Water Well wins"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("games_played", moduleConfig.getStringFrom("language.yml", "stats.labels.games_played", "Games Played"), moduleConfig.getStringFrom("language.yml", "stats.descriptions.games_played", "Water Well games played"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("water_landings", moduleConfig.getStringFrom("language.yml", "stats.labels.water_landings", "Water landings"), moduleConfig.getStringFrom("language.yml", "stats.descriptions.water_landings", "Successful water landings"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("total_score", moduleConfig.getStringFrom("language.yml", "stats.labels.total_score", "Total score"), moduleConfig.getStringFrom("language.yml", "stats.descriptions.total_score", "Total points earned from water landings"), StatScope.MODULE));
    }

    public void recordWaterLanding(Player player) {
        if (statsAPI == null) {
            return;
        }

        statsAPI.addModuleStat(player, moduleInfo.getId(), "water_landings", 1);
        statsAPI.addModuleStat(player, moduleInfo.getId(), "total_score", 2);
    }

    public void recordWin(Player player) {
        if (statsAPI == null) {
            return;
        }

        statsAPI.addModuleStat(player, moduleInfo.getId(), "wins", 1);
        statsAPI.addGlobalStat(player, "wins", 1);
    }

    public void recordGamesPlayed(Collection<Player> players) {
        if (statsAPI == null) {
            return;
        }

        for (Player player : players) {
            statsAPI.addModuleStat(player, moduleInfo.getId(), "games_played", 1);
        }
    }
}
