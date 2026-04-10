package net.blueva.arcade.modules.water_well.state;

import net.blueva.arcade.api.game.GameContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaterWellStateRegistry {

    private final Map<Integer, WaterWellArenaState> arenas = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerArenas = new ConcurrentHashMap<>();

    public void registerArena(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();
        arenas.put(arenaId, new WaterWellArenaState(context));
        context.getPlayers().forEach(player -> playerArenas.put(player.getUniqueId(), arenaId));
    }

    public Integer getArenaId(Player player) {
        return playerArenas.get(player.getUniqueId());
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getContext(int arenaId) {
        WaterWellArenaState state = arenas.get(arenaId);
        return state != null ? state.getContext() : null;
    }

    public WaterWellArenaState getArenaState(int arenaId) {
        return arenas.get(arenaId);
    }

    public boolean isEnded(int arenaId) {
        WaterWellArenaState state = arenas.get(arenaId);
        return state != null && state.isEnded();
    }

    public boolean markEnded(int arenaId) {
        WaterWellArenaState state = arenas.get(arenaId);
        if (state == null || state.isEnded()) {
            return false;
        }

        state.markEnded();
        return true;
    }

    public boolean markWinner(int arenaId, UUID winner) {
        WaterWellArenaState state = arenas.get(arenaId);
        if (state == null || state.getWinner() != null) {
            return false;
        }

        state.setWinner(winner);
        return true;
    }

    public void restoreBlocks(int arenaId) {
        WaterWellArenaState state = arenas.get(arenaId);
        if (state != null) {
            state.restoreBlocks();
        }
    }

    /**
     * Returns the top players sorted by score descending.
     */
    public List<Map.Entry<UUID, Integer>> getTopPlayers(int arenaId, int limit) {
        WaterWellArenaState state = arenas.get(arenaId);
        if (state == null) {
            return List.of();
        }

        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(state.getPlayerScores().entrySet());
        sorted.sort(Map.Entry.<UUID, Integer>comparingByValue().reversed());

        return sorted.subList(0, Math.min(sorted.size(), limit));
    }

    public int getPlayerPosition(int arenaId, UUID playerId) {
        List<Map.Entry<UUID, Integer>> top = getTopPlayers(arenaId, Integer.MAX_VALUE);
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).getKey().equals(playerId)) {
                return i + 1;
            }
        }
        return top.size() + 1;
    }

    public void clearArena(int arenaId) {
        WaterWellArenaState state = arenas.remove(arenaId);
        if (state != null) {
            state.getContext().getPlayers().forEach(player -> playerArenas.remove(player.getUniqueId()));
        }
    }

    public void clearAll() {
        arenas.clear();
        playerArenas.clear();
    }

    public void cancelAllSchedulers(String moduleId) {
        for (WaterWellArenaState state : arenas.values()) {
            state.getContext().getSchedulerAPI().cancelModuleTasks(moduleId);
        }
    }
}
