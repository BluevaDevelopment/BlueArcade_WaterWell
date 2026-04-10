package net.blueva.arcade.modules.water_well.support.messaging;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.module.ModuleInfo;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class WaterWellMessagingService {

    private final ModuleInfo moduleInfo;
    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;

    public WaterWellMessagingService(ModuleInfo moduleInfo,
                                     ModuleConfigAPI moduleConfig,
                                     CoreConfigAPI coreConfig) {
        this.moduleInfo = moduleInfo;
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
    }

    public void sendDescription(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        List<String> description = moduleConfig.getStringListFrom("language.yml", "description");

        for (Player player : context.getPlayers()) {
            for (String line : description) {
                context.getMessagesAPI().sendRaw(player, line);
            }
        }
    }

    public void sendCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  int secondsLeft) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) continue;

            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.countdown"));

            String title = coreConfig.getLanguage("titles.starting_game.title")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            String subtitle = coreConfig.getLanguage("titles.starting_game.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 5);
        }
    }

    public void sendCountdownFinished(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) continue;

            String title = coreConfig.getLanguage("titles.game_started.title")
                    .replace("{game_display_name}", moduleInfo.getName());

            String subtitle = coreConfig.getLanguage("titles.game_started.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName());

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 20);
            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.start"));
        }
    }

    public void sendActionBar(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                              Player player,
                              int timeLeft) {
        String actionBarTemplate = coreConfig.getLanguage("action_bar.in_game.global");
        if (actionBarTemplate == null) {
            return;
        }

        String actionBarMessage = actionBarTemplate
                .replace("{time}", String.valueOf(timeLeft))
                .replace("{round}", String.valueOf(context.getCurrentRound()))
                .replace("{round_max}", String.valueOf(context.getMaxRounds()));
        context.getMessagesAPI().sendActionBar(player, actionBarMessage);
    }

    public void sendWaterLanding(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 Player player) {
        String message = moduleConfig.getStringFrom("language.yml", "messages.water_landing");
        if (message != null) {
            context.getMessagesAPI().sendRaw(player, message);
        }
        context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.respawn"));
    }

    public void sendMissedLanding(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                  Player player) {
        String message = moduleConfig.getStringFrom("language.yml", "messages.missed_landing");
        if (message != null) {
            context.getMessagesAPI().sendRaw(player, message);
        }
    }
}
