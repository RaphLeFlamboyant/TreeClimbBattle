package me.flamboyant.treeclimbbattle;

import me.flamboyant.FlamboyantPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends FlamboyantPlugin {

    @Override
    public void onEnable() {
        super.onEnable();

        CommandsDispatcher commandDispatcher = new CommandsDispatcher();

        getCommand("f_tree_climb_battle").setExecutor(commandDispatcher);
    }
}
