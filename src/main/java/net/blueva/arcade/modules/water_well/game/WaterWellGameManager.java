package net.blueva.arcade.modules.water_well.game;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.modules.water_well.state.WaterWellStateRegistry;
import net.blueva.arcade.modules.water_well.state.WaterWellArenaState;
import net.blueva.arcade.modules.water_well.support.loadout.WaterWellLoadoutService;
import net.blueva.arcade.modules.water_well.support.messaging.WaterWellMessagingService;
import net.blueva.arcade.modules.water_well.support.stats.WaterWellStatsService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WaterWellGameManager {

    private final ModuleInfo moduleInfo;
    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final WaterWellStatsService statsService;
    private final WaterWellLoadoutService loadoutService;
    private final WaterWellMessagingService messagingService;
    private final WaterWellStateRegistry stateRegistry;

    public WaterWellGameManager(ModuleInfo moduleInfo,
                                ModuleConfigAPI moduleConfig,
                                CoreConfigAPI coreConfig,
                                WaterWellStatsService statsService) {
        this.moduleInfo = moduleInfo;
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.statsService = statsService;
        this.stateRegistry = new WaterWellStateRegistry();
        this.loadoutService = new WaterWellLoadoutService(moduleConfig);
        this.messagingService = new WaterWellMessagingService(moduleInfo, moduleConfig, coreConfig);
    }

    public void handleStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        stateRegistry.registerArena(context);
        messagingService.sendDescription(context);
    }

    public void handleCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    int secondsLeft) {
        messagingService.sendCountdownTick(context, secondsLeft);
    }

    public void handleCountdownFinish(
            GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        messagingService.sendCountdownFinished(context);
    }

    public void handleGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        startGameTimer(context);

        for (Player player : context.getPlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            loadoutService.giveStartingItems(player);
            loadoutService.applyStartingEffects(player);
            context.getScoreboardAPI().showModuleScoreboard(player);
        }
    }

    private void startGameTimer(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        Integer gameTime = context.getDataAccess().getGameData("basic.time", Integer.class);
        if (gameTime == null || gameTime == 0) {
            gameTime = 180;
        }

        final int[] timeLeft = {gameTime};

        String taskId = "arena_" + arenaId + "_water_well_timer";

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (stateRegistry.isEnded(arenaId)) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            timeLeft[0]--;

            List<Player> allPlayers = context.getPlayers();

            if (timeLeft[0] <= 0) {
                endGameOnce(context);
                return;
            }

            for (Player player : allPlayers) {
                if (!player.isOnline()) {
                    continue;
                }

                messagingService.sendActionBar(context, player, timeLeft[0]);

                Map<String, String> customPlaceholders = getCustomPlaceholders(player);
                customPlaceholders.put("time", String.valueOf(timeLeft[0]));
                customPlaceholders.put("round", String.valueOf(context.getCurrentRound()));
                customPlaceholders.put("round_max", String.valueOf(context.getMaxRounds()));
                customPlaceholders.put("spectators", String.valueOf(context.getSpectators().size()));

                context.getScoreboardAPI().update(player, customPlaceholders);
            }
        }, 0L, 20L);
    }

    private void endGameOnce(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        if (!stateRegistry.markEnded(arenaId)) {
            return;
        }

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        // Winner is the player with the highest score; all players are sorted by score descending
        List<Map.Entry<UUID, Integer>> topPlayers = stateRegistry.getTopPlayers(arenaId, Integer.MAX_VALUE);
        if (!topPlayers.isEmpty()) {
            UUID winnerId = topPlayers.get(0).getKey();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null && winner.isOnline()) {
                context.setWinner(winner);
                handleWin(winner);
            }

            // Register remaining players for the podium in score order
            for (int i = 1; i < topPlayers.size(); i++) {
                UUID playerId = topPlayers.get(i).getKey();
                Player podiumPlayer = Bukkit.getPlayer(playerId);
                if (podiumPlayer != null && podiumPlayer.isOnline()
                        && !context.getSpectators().contains(podiumPlayer)) {
                    context.finishPlayer(podiumPlayer);
                }
            }
        }

        context.endGame();
    }

    public void handleEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        context.getSchedulerAPI().cancelArenaTasks(arenaId);

        stateRegistry.restoreBlocks(arenaId);
        statsService.recordGamesPlayed(context.getPlayers());
        stateRegistry.clearArena(arenaId);
    }

    public void handleDisable() {
        stateRegistry.cancelAllSchedulers(moduleInfo.getId());
        stateRegistry.clearAll();
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContext(Player player) {
        Integer arenaId = stateRegistry.getArenaId(player);
        if (arenaId == null) {
            return null;
        }
        return stateRegistry.getContext(arenaId);
    }

    public WaterWellStateRegistry getStateRegistry() {
        return stateRegistry;
    }

    public void handleWaterLanding(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                   Player player) {
        statsService.recordWaterLanding(player);
        messagingService.sendWaterLanding(context, player);
    }

    public void handleMissedLanding(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    Player player) {
        messagingService.sendMissedLanding(context, player);
    }

    public void handlePlayerRespawn(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    Player player) {
        loadoutService.applyRespawnEffects(player);
        context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.respawn"));
    }

    public void handleWin(Player player) {
        Integer arenaId = stateRegistry.getArenaId(player);
        if (arenaId == null) {
            return;
        }

        if (stateRegistry.markWinner(arenaId, player.getUniqueId())) {
            statsService.recordWin(player);
        }
    }

    public Map<String, String> getCustomPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        if (context == null) {
            return placeholders;
        }

        int arenaId = context.getArenaId();
        WaterWellArenaState state = stateRegistry.getArenaState(arenaId);
        if (state == null) {
            return placeholders;
        }

        // Top 5 players
        List<Map.Entry<UUID, Integer>> topPlayers = stateRegistry.getTopPlayers(arenaId, 5);
        for (int i = 0; i < 5; i++) {
            int rank = i + 1;
            if (i < topPlayers.size()) {
                UUID playerId = topPlayers.get(i).getKey();
                int score = topPlayers.get(i).getValue();
                Player topPlayer = Bukkit.getPlayer(playerId);
                String name = topPlayer != null ? topPlayer.getName() : "???";
                placeholders.put("place_" + rank, name);
                placeholders.put("score_" + rank, String.valueOf(score));
            } else {
                placeholders.put("place_" + rank, "-");
                placeholders.put("score_" + rank, "0");
            }
        }

        // Current player score and position
        placeholders.put("player_score", String.valueOf(state.getScore(player.getUniqueId())));
        placeholders.put("player_position", String.valueOf(stateRegistry.getPlayerPosition(arenaId, player.getUniqueId())));

        return placeholders;
    }
}
