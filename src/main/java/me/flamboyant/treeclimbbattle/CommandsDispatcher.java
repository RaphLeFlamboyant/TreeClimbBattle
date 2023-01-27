package me.flamboyant.treeclimbbattle;

import me.flamboyant.configurable.gui.ConfigurablePluginListener;
import me.flamboyant.treeclimbbattle.listeners.GameListener;
import me.flamboyant.utils.ILaunchablePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandsDispatcher implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String msg, String[] args) {
        if(sender instanceof Player)
        {
            Player commandSender = (Player) sender;
            ILaunchablePlugin pluginToLaunch = null;
            switch (cmd.getName())
            {
                case "f_tree_climb_battle":
                    pluginToLaunch = GameListener.getInstance();
                    break;
                default :
                    break;
            }
            if (pluginToLaunch != null) handleTwist(commandSender, pluginToLaunch);
            return true;
        }
        return false;
    }

    private void handleTwist(Player sender, ILaunchablePlugin twist) {
        if (twist.isRunning()) {
            sender.sendMessage(ChatColor.RED + "Plugin stopped");
            twist.stop();
            return;
        }

        twist.resetParameters();

        if (!ConfigurablePluginListener.getInstance().isLaunched())
            ConfigurablePluginListener.getInstance().launch(twist, sender);

        sender.sendMessage("Plugin started");
    }
}
