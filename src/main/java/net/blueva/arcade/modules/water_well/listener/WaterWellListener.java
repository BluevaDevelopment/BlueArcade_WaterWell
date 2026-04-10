package net.blueva.arcade.modules.water_well.listener;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.water_well.game.WaterWellGameManager;
import net.blueva.arcade.modules.water_well.state.WaterWellArenaState;
import net.blueva.arcade.modules.water_well.state.WaterWellStateRegistry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class WaterWellListener implements Listener {

    private static final Material[] WOOL_COLORS = {
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
            Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
            Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL,
            Material.BLACK_WOOL
    };

    private final WaterWellGameManager gameManager;

    public WaterWellListener(WaterWellGameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        if (context.getPhase() != GamePhase.PLAYING) {
            if (!context.isInsideBounds(event.getTo())) {
                player.setFallDistance(0);
                context.respawnPlayer(player);
                gameManager.handlePlayerRespawn(context, player);
                return;
            }

            if (hasChangedBlock(event)) {
                player.teleport(event.getFrom());
            }
            return;
        }

        if (!context.isInsideBounds(event.getTo())) {
            player.setFallDistance(0);
            context.respawnPlayer(player);
            gameManager.handlePlayerRespawn(context, player);
            return;
        }

        if (!hasChangedBlock(event)) {
            return;
        }

        // During PLAYING phase: detect landings
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to.getY() > from.getY()) {
            return;
        }

        WaterWellStateRegistry stateRegistry = gameManager.getStateRegistry();
        Integer arenaIdObj = stateRegistry.getArenaId(player);
        if (arenaIdObj == null) {
            return;
        }

        WaterWellArenaState state = stateRegistry.getArenaState(arenaIdObj);
        if (state == null || state.isEnded()) {
            return;
        }

        Block fromBlockAtFeet = from.getBlock();
        Block toBlockAtFeet = to.getBlock();

        // Score +2 only on transition INTO water to avoid duplicate rewards while
        // the player is still moving inside the same/deeper water column.
        boolean enteredWater = !isWater(fromBlockAtFeet) && isWater(toBlockAtFeet);
        if (!enteredWater) {
            return;
        }

        // +2 points: landed in water
        Location blockLoc = toBlockAtFeet.getLocation();

        state.trackBlock(blockLoc, toBlockAtFeet.getType());
        toBlockAtFeet.setType(getRandomWool());

        state.addScore(player.getUniqueId(), 2);
        gameManager.handleWaterLanding(context, player);

        // Reset fall distance and teleport to random spawn
        player.setFallDistance(0);
        context.respawnPlayer(player);
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        if (context.getPhase() != GamePhase.PLAYING) {
            event.setCancelled(true);
            player.setFallDistance(0);
            return;
        }

        WaterWellStateRegistry stateRegistry = gameManager.getStateRegistry();
        Integer arenaIdObj = stateRegistry.getArenaId(player);
        if (arenaIdObj == null) {
            event.setCancelled(true);
            player.setFallDistance(0);
            return;
        }

        WaterWellArenaState state = stateRegistry.getArenaState(arenaIdObj);
        if (state == null || state.isEnded()) {
            event.setCancelled(true);
            player.setFallDistance(0);
            return;
        }

        // Falling damage means the player did not land in water -> guaranteed miss (-1)
        state.addScore(player.getUniqueId(), -1);
        gameManager.handleMissedLanding(context, player);

        event.setCancelled(true);
        player.setFallDistance(0);
        context.respawnPlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context =
                gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        // Prevent all block breaking in Water Well
        event.setCancelled(true);
    }

    private Material getRandomWool() {
        return WOOL_COLORS[ThreadLocalRandom.current().nextInt(WOOL_COLORS.length)];
    }

    private boolean hasChangedBlock(PlayerMoveEvent event) {
        if (event.getFrom() == null || event.getTo() == null) {
            return false;
        }

        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                || !event.getFrom().getWorld().equals(event.getTo().getWorld());
    }

    private boolean isWater(Block block) {
        return block.getType() == Material.WATER;
    }
}
