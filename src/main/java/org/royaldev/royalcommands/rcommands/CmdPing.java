package org.royaldev.royalcommands.rcommands;

import org.royaldev.royalcommands.MessageColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.royaldev.royalcommands.RUtils;
import org.royaldev.royalcommands.RoyalCommands;

public class CmdPing implements CommandExecutor {

    private RoyalCommands plugin;

    public CmdPing(RoyalCommands instance) {
        this.plugin = instance;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("ping")) {
            if (!plugin.ah.isAuthorized(cs, "rcmds.ping")) {
                RUtils.dispNoPerms(cs);
                return true;
            }
            if (!plugin.nmsFace.hasSupport() || (!(cs instanceof Player) && args.length < 1)) {
                cs.sendMessage(MessageColor.POSITIVE + "Pong!");
                return true;
            }
            if (args.length > 0) {
                if (!plugin.ah.isAuthorized(cs, "rcmds.others.ping")) {
                    RUtils.dispNoPerms(cs);
                    return true;
                }
                Player p = plugin.getServer().getPlayer(args[0]);
                if (p == null || plugin.isVanished(p, cs)) {
                    cs.sendMessage(MessageColor.NEGATIVE + "That player does not exist!");
                    return true;
                }
                int ping = plugin.nmsFace.getPing(p);
                String possessive = (p.getName().endsWith("s")) ? "'" : "'s";
                cs.sendMessage(MessageColor.NEUTRAL + p.getName() + possessive + MessageColor.POSITIVE + " ping: " + MessageColor.NEUTRAL + ping + "ms");
                return true;
            }
            Player p = (Player) cs;
            int ping = plugin.nmsFace.getPing(p);
            p.sendMessage(MessageColor.POSITIVE + "Your ping: " + MessageColor.NEUTRAL + ping + "ms");
            return true;
        }
        return false;
    }
}
