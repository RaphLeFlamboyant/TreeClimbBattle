package me.flamboyant.treeclimbbattle.listeners;

import me.flamboyant.configurable.parameters.AParameter;
import me.flamboyant.utils.ChatHelper;
import me.flamboyant.utils.Common;
import me.flamboyant.utils.ILaunchablePlugin;
import me.flamboyant.utils.ItemHelper;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GameListener implements ILaunchablePlugin, Listener {
    private HashMap<HumanEntity, Location> playersLocation = new HashMap<>();
    private World customWorld;
    private Location zeroLocation;
    private boolean running;
    private BukkitTask runningTask;

    private static GameListener instance;

    public static GameListener getInstance() {
        if (instance == null) {
            instance = new GameListener();
        }

        return instance;
    }

    protected GameListener() {
    }

    @Override
    public boolean start() {
        if (running) {
            return false;
        }

        Bukkit.getLogger().info("Starting Tree Climb Battle");
        running = true;

        customWorld = new WorldCreator("TreeClimbBattleWorld")
                .environment(World.Environment.NORMAL)
                .type(WorldType.FLAT)
                .generatorSettings("{\"layers\": [{\"block\": \"bedrock\", \"height\": 1}, {\"block\": \"grass_block\", \"height\": 1}], \"biome\":\"plains\"}")
                .generateStructures(false).createWorld();

        runningTask = Bukkit.getScheduler().runTaskLater(Common.plugin, () -> launchAfterCountdown(5), 20);
        Common.server.getPluginManager().registerEvents(this, Common.plugin);

        return true;
    }

    @Override
    public boolean stop() {
        if (!running) {
            return false;
        }

        BlockPlaceEvent.getHandlerList().unregister(this);
        CreatureSpawnEvent.getHandlerList().unregister(this);
        ProjectileHitEvent.getHandlerList().unregister(this);
        ProjectileLaunchEvent.getHandlerList().unregister(this);
        EntityDamageEvent.getHandlerList().unregister(this);
        FoodLevelChangeEvent.getHandlerList().unregister(this);
        PlayerDropItemEvent.getHandlerList().unregister(this);
        PlayerSwapHandItemsEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);

        Bukkit.getScheduler().cancelTask(runningTask.getTaskId());

        for (HumanEntity player : playersLocation.keySet()) {
            player.getInventory().clear();
            player.teleport(playersLocation.get(player));
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(10);
        }

        Bukkit.getScheduler().runTaskLater(Common.plugin, () -> {
                    Bukkit.unloadWorld(customWorld, false);
                    Bukkit.getScheduler().runTaskLater(Common.plugin, () -> deleteFolder(new File("TreeClimbBattleWorld")), 5);
                }, 10 * 20);

        Bukkit.getLogger().info("Stopping Tree Climb Battle");
        running = false;

        return true;
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files != null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean canModifyParametersOnTheFly() {
        return false;
    }

    @Override
    public void resetParameters() {
    }

    @Override
    public List<AParameter> getParameters() {
        return new ArrayList<>();
    }


    private List<Material> woodBlocks;
    private List<Material> getWoodMaterial() {
        if (woodBlocks == null) {
            List<String> woodTypes = Arrays.stream(Material.values()).filter(m -> m.toString().contains("PLANK")).map(m -> m.toString().split("_")[0]).collect(Collectors.toList());
            woodBlocks = Arrays.stream(Material.values()).filter(m -> woodTypes.contains(m.toString().split("_")[0])).collect(Collectors.toList());
        }

        return woodBlocks;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getWorld() != customWorld) return;
        if (getWoodMaterial().contains(event.getBlock().getType()) || event.getBlock().getType() == Material.DIRT) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity().getWorld() != customWorld) return;
        if (event.getEntity().getType() != EntityType.SNOWBALL) return;

        customWorld.generateTree(event.getEntity().getLocation(), TreeType.values()[Common.rng.nextInt(TreeType.values().length)]);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntityType() != EntityType.SNOWBALL) return;
        if (!playersLocation.containsKey(event.getEntity().getShooter())) return;

        Player player = (Player) event.getEntity().getShooter();
        player.setCooldown(Material.SNOWBALL, 1 * 20);
        player.getInventory().addItem(snowBallItem(1));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER) return;
        Player player = (Player) event.getEntity();
        if (!playersLocation.containsKey(player)) return;
        if (player.getHealth() - event.getFinalDamage() > 0) return;

        event.setCancelled(true);
        player.teleport(zeroLocation);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(10);

        Bukkit.broadcastMessage(player.getDisplayName() + " is dead by the violence of the Tree Climb Battle");
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!playersLocation.containsKey(event.getEntity())) return;;
        if (!ItemHelper.isSameItemKind(event.getItem(), foodItem(1))) return;

        event.getEntity().getInventory().addItem(foodItem(1));
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (ItemHelper.isSameItemKind(event.getItemDrop().getItemStack(), snowBallItem(1))
                || ItemHelper.isSameItemKind(event.getItemDrop().getItemStack(), foodItem(1)))
            event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (ItemHelper.isSameItemKind(event.getOffHandItem(), snowBallItem(1))
                || ItemHelper.isSameItemKind(event.getOffHandItem(), foodItem(1)))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (ItemHelper.isSameItemKind(event.getCurrentItem(), snowBallItem(1))
                || ItemHelper.isSameItemKind(event.getCurrentItem(), foodItem(1)))
            event.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity().getLocation().getWorld() != customWorld) return;

        event.setCancelled(true);
    }

    private void launchAfterCountdown(int countdown) {
        if (countdown > 0) {
            for (Player player : Common.server.getOnlinePlayers()) {
                player.sendTitle("" + countdown, "", 0, 20, 0);
            }
            runningTask = Bukkit.getScheduler().runTaskLater(Common.plugin, () -> launchAfterCountdown(countdown - 1), 20);
        }
        else {
            zeroLocation = new Location(customWorld, 0, customWorld.getHighestBlockYAt(0, 0), 0);
            for (Player player : Common.server.getOnlinePlayers()) {
                playersLocation.put(player, player.getLocation());
                player.getInventory().clear();
                player.getInventory().addItem(snowBallItem(16));
                player.getInventory().addItem(foodItem(16));
                player.teleport(zeroLocation);
            }

            Common.server.getWorld("TreeClimbBattleWorld").getWorldBorder().setSize(10 * 16);

            runningTask = Bukkit.getScheduler().runTaskTimer(Common.plugin, this::checkWinTask, 20, 20);
            Bukkit.broadcastMessage(ChatHelper.titledMessage("La partie commence", "Plante des arbres pour atteindre le sommet ! Le premier à la couche 300 a gagné !"));
        }
    }

    private void checkWinTask() {
        for (HumanEntity player : playersLocation.keySet()) {
            if (player.getLocation().getBlockY() >= 300) {
                Bukkit.broadcastMessage(ChatHelper.titledMessage("Terminé", player.getName() + " a gagné la partie !"));
                stop();
                return;
            }
        }
    }

    private ItemStack foodItem(int quantity) {
        return ItemHelper.generateItem(Material.GOLDEN_CARROT, quantity, "Bouffe infinie", new ArrayList<>(), true, Enchantment.ARROW_FIRE, true, true);
    }

    private ItemStack snowBallItem(int quantity) {
        return ItemHelper.generateItem(Material.SNOWBALL, quantity, "Boule à arbre", Arrays.asList("Génère un arbre là où la boule est lancée"), true, Enchantment.ARROW_FIRE, true, true);
    }
}
