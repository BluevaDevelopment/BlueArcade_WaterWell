package net.blueva.arcade.modules.water_well.state;

import net.blueva.arcade.api.game.GameContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaterWellArenaState {

    private final GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context;
    private final Map<Location, Material> changedBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerScores = new ConcurrentHashMap<>();
    private boolean ended = false;
    private UUID winner = null;

    public WaterWellArenaState(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        this.context = context;
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getContext() {
        return context;
    }

    public Map<Location, Material> getChangedBlocks() {
        return changedBlocks;
    }

    public Map<UUID, Integer> getPlayerScores() {
        return playerScores;
    }

    public int getScore(UUID playerId) {
        return playerScores.getOrDefault(playerId, 0);
    }

    public void addScore(UUID playerId, int amount) {
        playerScores.merge(playerId, amount, Integer::sum);
    }

    public void trackBlock(Location location, Material originalMaterial) {
        changedBlocks.putIfAbsent(location, originalMaterial);
    }

    public void restoreBlocks() {
        for (Map.Entry<Location, Material> entry : changedBlocks.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() != null) {
                loc.getBlock().setType(Material.WATER);
            }
        }
        changedBlocks.clear();
    }

    public boolean isEnded() {
        return ended;
    }

    public void markEnded() {
        this.ended = true;
    }

    public UUID getWinner() {
        return winner;
    }

    public void setWinner(UUID winner) {
        this.winner = winner;
    }
}
