package org.royaldev.royalcommands.rcommands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalcommands.RoyalCommands;
import org.royaldev.royalcommands.RUtils;

public class Jump implements CommandExecutor {

    RoyalCommands plugin;

    public Jump(RoyalCommands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label,
                             String[] args) {
        if (cmd.getName().equalsIgnoreCase("jump")) {
            if (!plugin.isAuthorized(cs, "rcmds.jump")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!(cs instanceof Player)) {
                cs.sendMessage(ChatColor.RED + "This command is only available to players!");
                return true;
            }
            Player p = (Player) cs;
            Block bb = RUtils.getTarget(p);
            if (bb == null) {
                cs.sendMessage(ChatColor.RED + "Can't jump there!");
                return true;
            }
            Location bLoc = new Location(p.getWorld(), bb.getLocation().getX(), bb.getLocation().getY() + 1, bb.getLocation().getZ(), p.getLocation().getYaw(), p.getLocation().getPitch());
            Back.backdb.put(p, p.getLocation());
            p.teleport(bLoc);
            return true;

        }
        return false;
    }

}