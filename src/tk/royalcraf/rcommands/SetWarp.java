package tk.royalcraf.rcommands;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import tk.royalcraf.royalcommands.RoyalCommands;

public class SetWarp implements CommandExecutor {

	RoyalCommands plugin;

	public SetWarp(RoyalCommands plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender cs, Command cmd, String label,
			String[] args) {
		if (cmd.getName().equalsIgnoreCase("setwarp")) {
			if (!plugin.isAuthorized(cs, "rcmds.setwarp")) {
				cs.sendMessage(ChatColor.RED
						+ "You don't have permission for that!");
				plugin.log.warning("[RoyalCommands] " + cs.getName()
						+ " was denied access to the command!");
				return true;
			}

			if (args.length < 1) {
				cs.sendMessage(cmd.getDescription());
				return false;
			}

			Player p = null;

			if (!(cs instanceof Player)) {
				cs.sendMessage(ChatColor.RED
						+ "This command is only available to players!");
				return true;
			} else {
				p = (Player) cs;
			}

			double locX = p.getLocation().getX();
			double locY = p.getLocation().getY();
			double locZ = p.getLocation().getZ();
			Float locYaw = p.getLocation().getYaw();
			Float locPitch = p.getLocation().getPitch();
			String locW = p.getWorld().getName();

			File pconfl = new File(plugin.getDataFolder() + "/warps.yml");
			if (pconfl.exists()) {
				FileConfiguration pconf = YamlConfiguration
						.loadConfiguration(pconfl);
				pconf.set("warps." + args[0] + ".set", true);
				pconf.set("warps." + args[0] + ".x", locX);
				pconf.set("warps." + args[0] + ".y", locY);
				pconf.set("warps." + args[0] + ".z", locZ);
				pconf.set("warps." + args[0] + ".pitch", locPitch.toString());
				pconf.set("warps." + args[0] + ".yaw", locYaw.toString());
				pconf.set("warps." + args[0] + ".w", locW);
				try {
					pconf.save(pconfl);
				} catch (IOException e) {
					e.printStackTrace();
				}
				p.sendMessage(ChatColor.BLUE + "Warp \"" + ChatColor.GRAY
						+ args[0] + ChatColor.BLUE + "\" set.");
				return true;
			}
		}
		return false;
	}

}
