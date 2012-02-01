package org.royaldev.royalcommands.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.inventory.ItemStack;
import org.royaldev.royalcommands.PConfManager;
import org.royaldev.royalcommands.RUtils;
import org.royaldev.royalcommands.RoyalCommands;
import org.royaldev.royalcommands.rcommands.*;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class RoyalCommandsPlayerListener implements Listener {

    public static RoyalCommands plugin;

    public RoyalCommandsPlayerListener(RoyalCommands instance) {
        plugin = instance;
    }

    Logger log = Logger.getLogger("Minecraft");

    public Double getCharge(String line) {
        if (!line.isEmpty()) {
            Double amount;
            try {
                amount = Double.parseDouble(line);
            } catch (Exception ex) {
                return null;
            }
            if (amount < 0) {
                return null;
            }
            return amount;
        }
        return (double) -1;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        if (plugin.showcommands) {
            log.info("[PLAYER_COMMAND] " + event.getPlayer().getName() + ": " + event.getMessage());
        }
        if (PConfManager.getPValBoolean(event.getPlayer(), "muted")) {
            for (String command : plugin.muteCmds) {
                if (event.getMessage().toLowerCase().startsWith(command.toLowerCase() + " ") || event.getMessage().equalsIgnoreCase(command.toLowerCase())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "You are muted.");
                    log.info("[RoyalCommands] " + event.getPlayer().getName() + " tried to use that command, but is muted.");
                    event.setCancelled(true);
                }
            }
        }
        if (PConfManager.getPValBoolean(event.getPlayer(), "jailed")) {
            event.getPlayer().sendMessage(ChatColor.RED + "You are jailed.");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (Afk.afkdb.containsKey(event.getPlayer())) {
            plugin.getServer().broadcastMessage(event.getPlayer().getName() + " is no longer AFK.");
            Afk.afkdb.remove(event.getPlayer());
        }
        if (PConfManager.getPValBoolean(event.getPlayer(), "muted")) {
            event.setFormat("");
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You are muted.");
            plugin.log.info("[RoyalCommands] " + event.getPlayer().getName() + " tried to speak, but has been muted.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) return;
        if (Afk.afkdb.containsKey(event.getPlayer())) {
            plugin.getServer().broadcastMessage(event.getPlayer().getName() + " is no longer AFK.");
            Afk.afkdb.remove(event.getPlayer());
        }
        if (PConfManager.getPValBoolean(event.getPlayer(), "frozen")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        //if (event.isCancelled()) return; <- breaks everything
        if (PConfManager.getPValBoolean(event.getPlayer(), "jailed")) {
            event.setCancelled(true);
        }
        Action act = event.getAction();
        if (act.equals(Action.LEFT_CLICK_AIR) || act.equals(Action.RIGHT_CLICK_AIR) || act.equals(Action.LEFT_CLICK_BLOCK) || act.equals(Action.RIGHT_CLICK_BLOCK)) {
            ItemStack id = event.getItem();
            if (id != null) {
                int idn = id.getTypeId();
                if (idn != 0) {
                    List<String> cmds = PConfManager.getPValStringList(event.getPlayer(), "assign." + idn);
                    if (cmds != null) {
                        for (String s : cmds) {
                            if (s.toLowerCase().trim().startsWith("c:")) {
                                event.getPlayer().chat(s.trim().substring(2));
                            } else {
                                event.getPlayer().performCommand(s.trim());
                            }
                        }
                    }
                }
            }
        }
        if (PConfManager.getPValBoolean(event.getPlayer(), "frozen")) {
            event.setCancelled(true);
        }
        if (plugin.buildPerm) {
            if (!plugin.isAuthorized(event.getPlayer(), "rcmds.build")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler()
    public void onInt(PlayerInteractEvent e) {
        if (e.isCancelled()) return;
        if (e.getClickedBlock() == null) return;
        if (!(e.getClickedBlock().getState() instanceof Sign)) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Sign s = (Sign) e.getClickedBlock().getState();
        if (e.getPlayer() == null) return;
        Player p = e.getPlayer();
        String line1 = ChatColor.stripColor(s.getLine(0));
        String line2 = ChatColor.stripColor(s.getLine(1));
        String line3 = ChatColor.stripColor(s.getLine(2));
        String line4 = ChatColor.stripColor(s.getLine(3));

        //Warp signs
        if (line1.equalsIgnoreCase("[warp]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.use.warp")) {
                RUtils.dispNoPerms(p);
                return;
            }
            if (line2.isEmpty()) {
                s.setLine(0, ChatColor.RED + line1);
                p.sendMessage(ChatColor.RED + "No warp specified.");
                return;
            }

            Double charge = getCharge(line3);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                s.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                s.setLine(2, ChatColor.DARK_GREEN + line3);
                boolean did = RUtils.chargePlayer(p, charge);
                if (!did) return;
            }

            Location warpLoc = Warp.pWarp(p, plugin, line2.toLowerCase());
            if (warpLoc == null) {
                return;
            }
            Back.backdb.put(p, p.getLocation());
            p.teleport(warpLoc);
        }

        //Time signs
        if (line1.equalsIgnoreCase(ChatColor.stripColor("[time]"))) {
            if (!plugin.isAuthorized(p, "rcmds.sign.use.time")) {
                RUtils.dispNoPerms(p);
                return;
            }
            if (line2.isEmpty() || (Time.getValidTime(line2) == null)) {
                s.setLine(0, ChatColor.RED + line1);
                p.sendMessage(ChatColor.RED + "Invalid time specified!");
                return;
            }

            Double charge = getCharge(line3);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                s.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                s.setLine(2, ChatColor.DARK_GREEN + line3);
                boolean did = RUtils.chargePlayer(p, charge);
                if (!did) return;
            }

            long time = Time.getValidTime(line2);
            p.getWorld().setTime(time);
        }

        //Disposal signs
        if (line1.equalsIgnoreCase("[disposal]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.use.disposal")) {
                RUtils.dispNoPerms(p);
                return;
            }

            Double charge = getCharge(line2);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                s.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                s.setLine(1, ChatColor.DARK_GREEN + line2);
                boolean did = RUtils.chargePlayer(p, charge);
                if (!did) return;
            }

            RUtils.showEmptyChest(p);
        }

        //Heal signs
        if (line1.equalsIgnoreCase("[heal]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.use.heal")) {
                RUtils.dispNoPerms(p);
                return;
            }

            Double charge = getCharge(line2);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                s.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                s.setLine(1, ChatColor.DARK_GREEN + line2);
                boolean did = RUtils.chargePlayer(p, charge);
                if (!did) return;
            }

            p.setHealth(20);
        }

        //Weather signs
        if (line1.equalsIgnoreCase("[weather]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.use.weather")) {
                RUtils.dispNoPerms(p);
                return;
            }
            Double charge = getCharge(line3);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                s.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                s.setLine(2, ChatColor.DARK_GREEN + line3);
                boolean did = RUtils.chargePlayer(p, charge);
                if (!did) return;
            }
            Weather.changeWeather(p, line2.trim());
        }

        //Give signs
        if (line1.equalsIgnoreCase("[give]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.use.give")) {
                RUtils.dispNoPerms(p);
                return;
            }
            int amount;
            try {
                amount = Integer.parseInt(line3);
            } catch (Exception ex) {
                p.sendMessage(ChatColor.RED + "The amount was not a valid number!");
                return;
            }
            Double charge = getCharge(line4);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                s.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                s.setLine(3, ChatColor.DARK_GREEN + line4);
                boolean did = RUtils.chargePlayer(p, charge);
                if (!did) return;
            }
            Give.giveItemStandalone(p, plugin, line2, amount);
        }

        //Command signs
        if (line1.equalsIgnoreCase("[command]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.use.command")) {
                RUtils.dispNoPerms(p);
                return;
            }

            Double charge = getCharge(line3);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                s.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                s.setLine(2, ChatColor.DARK_GREEN + line3);
                boolean did = RUtils.chargePlayer(p, charge);
                if (!did) return;
            }
            if (line2.isEmpty()) {
                s.setLine(0, ChatColor.RED + line1);
                p.sendMessage(ChatColor.RED + "No command specified!");
                return;
            }
            p.performCommand(line2.trim());
        }
    }

    @EventHandler()
    public void onSignChange(SignChangeEvent e) {
        if (e.isCancelled()) return;
        if (e.getPlayer() == null) return;
        Player p = e.getPlayer();
        String line1 = ChatColor.stripColor(e.getLine(0));
        String line2 = ChatColor.stripColor(e.getLine(1));
        String line3 = ChatColor.stripColor(e.getLine(2));
        String line4 = ChatColor.stripColor(e.getLine(3));

        //Warp signs
        if (line1.equalsIgnoreCase("[warp]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.warp")) {
                RUtils.dispNoPerms(p);
                return;
            }
            if (line2.isEmpty()) {
                e.setLine(0, ChatColor.RED + line1);
                p.sendMessage(ChatColor.RED + "No warp specified.");
                return;
            }

            Double charge = getCharge(line3);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                e.setLine(2, ChatColor.DARK_GREEN + line3);
            }

            Location warpLoc = Warp.pWarp(p, plugin, line2.toLowerCase());
            if (warpLoc == null) {
                e.setLine(0, ChatColor.RED + line1);
                return;
            }
            e.setLine(0, ChatColor.BLUE + line1);
            p.sendMessage(ChatColor.BLUE + "Warp sign created successfully.");
        }

        //Time signs
        if (line1.equalsIgnoreCase("[time]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.time")) {
                RUtils.dispNoPerms(p);
                return;
            }
            if (line2.isEmpty() || (Time.getValidTime(line2)) == null) {
                e.setLine(0, ChatColor.RED + line1);
                return;
            }

            Double charge = getCharge(line3);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                e.setLine(2, ChatColor.DARK_GREEN + line3);
            }

            e.setLine(0, ChatColor.BLUE + line1);
            p.sendMessage(ChatColor.BLUE + "Time sign created successfully!");
        }

        //Disposal signs
        if (line1.equalsIgnoreCase("[disposal]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.disposal")) {
                RUtils.dispNoPerms(p);
                return;
            }

            Double charge = getCharge(line2);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                e.setLine(1, ChatColor.DARK_GREEN + line2);
            }

            e.setLine(0, ChatColor.BLUE + line1);
            p.sendMessage(ChatColor.BLUE + "Disposal sign created successfully!");
        }

        //Heal signs
        if (line1.equalsIgnoreCase("[heal]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.heal")) {
                RUtils.dispNoPerms(p);
                return;
            }

            Double charge = getCharge(line2);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                e.setLine(1, ChatColor.DARK_GREEN + line2);
            }

            e.setLine(0, ChatColor.BLUE + line1);
            p.sendMessage(ChatColor.BLUE + "Heal sign created successfully!");
        }

        //Weather signs
        if (line1.equalsIgnoreCase("[weather]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.weather")) {
                RUtils.dispNoPerms(p);
                return;
            }

            Double charge = getCharge(line3);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                e.setLine(2, ChatColor.DARK_GREEN + line3);
            }

            boolean valid = Weather.validWeather(line2.trim());
            if (!valid) {
                p.sendMessage(ChatColor.RED + "Invalid weather condition!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            }
            e.setLine(0, ChatColor.BLUE + line1);
            p.sendMessage(ChatColor.BLUE + "Weather sign created successfully!");
        }

        //Give signs
        if (line1.equalsIgnoreCase("[give]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.give")) {
                RUtils.dispNoPerms(p);
                return;
            }

            Double charge = getCharge(line4);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                e.setLine(3, ChatColor.DARK_GREEN + line4);
            }

            boolean valid = Give.validItem(line2.trim());
            if (!valid) {
                p.sendMessage(ChatColor.RED + "That item is invalid!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            }
            try {
                Integer.parseInt(line3);
            } catch (Exception ex) {
                p.sendMessage(ChatColor.RED + "The amount was not a valid number!");
                return;
            }
            e.setLine(0, ChatColor.BLUE + line1);
            p.sendMessage(ChatColor.BLUE + "Give sign created successfully!");
        }

        //Command signs
        if (line1.equalsIgnoreCase("[command]")) {
            if (!plugin.isAuthorized(p, "rcmds.sign.command")) {
                RUtils.dispNoPerms(p);
                return;
            }

            Double charge = getCharge(line3);
            if (charge == null) {
                p.sendMessage(ChatColor.RED + "The cost is invalid!");
                e.setLine(0, ChatColor.RED + line1);
                return;
            } else if (charge >= 0) {
                e.setLine(2, ChatColor.DARK_GREEN + line3);
            }
            if (line2.isEmpty()) {
                e.setLine(0, ChatColor.RED + line1);
                p.sendMessage(ChatColor.RED + "No command specified!");
                return;
            }
            p.sendMessage(ChatColor.BLUE + "Command sign created successfully.");
            e.setLine(0, ChatColor.BLUE + line1);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer().isBanned()) {
            String kickMessage;
            OfflinePlayer oplayer = plugin.getServer().getOfflinePlayer(event.getPlayer().getName());
            File oplayerconfl = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + oplayer.getName() + ".yml");
            if (oplayerconfl.exists()) {
                FileConfiguration oplayerconf = YamlConfiguration.loadConfiguration(oplayerconfl);
                kickMessage = oplayerconf.getString("banreason");
            } else {
                kickMessage = plugin.banMessage;
            }
            event.setKickMessage(kickMessage);
            event.disallow(Result.KICK_BANNED, kickMessage);
        }
        Player p = event.getPlayer();
        String dispname = PConfManager.getPValString(p, "dispname").trim();
        if (dispname == null || dispname.equals("")) {
            dispname = p.getName().trim();
        }
        p.setDisplayName(dispname);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        File datafile = new File(plugin.getDataFolder() + File.separator + "userdata" + File.separator + event.getPlayer().getName().toLowerCase() + ".yml");
        if (!datafile.exists()) {
            log.info("[RoyalCommands] Creating userdata for user " + event.getPlayer().getName() + ".");
            try {
                FileConfiguration out = YamlConfiguration.loadConfiguration(datafile);
                out.set("name", event.getPlayer().getName());
                String dispname = event.getPlayer().getDisplayName();
                if (dispname == null || dispname.equals("")) {
                    dispname = event.getPlayer().getName();
                }
                out.set("dispname", dispname);
                out.set("ip", event.getPlayer().getAddress().getAddress().toString().replace("/", ""));
                out.set("banreason", "");
                out.save(datafile);
                log.info("[RoyalCommands] Userdata creation finished.");
            } catch (Exception e) {
                log.severe("[RoyalCommands] Could not create userdata for user " + event.getPlayer().getName() + "!");
                e.printStackTrace();
            }
            if (plugin.useWelcome) {
                String welcomemessage = plugin.welcomeMessage;
                welcomemessage = welcomemessage.replace("{name}", event.getPlayer().getName());
                String dispname = event.getPlayer().getDisplayName();
                if (dispname == null || dispname.equals("")) {
                    dispname = event.getPlayer().getName();
                }
                welcomemessage = welcomemessage.replace("{dispname}", dispname);
                welcomemessage = welcomemessage.replace("{world}", event.getPlayer().getWorld().getName());
                plugin.getServer().broadcastMessage(welcomemessage);
            }
        } else {
            log.info("[RoyalCommands] Updating the IP for " + event.getPlayer().getName() + ".");
            String playerip = event.getPlayer().getAddress().getAddress().toString();
            playerip = playerip.replace("/", "");
            PConfManager.setPValString(event.getPlayer(), playerip, "ip");
        }
        if (plugin.motdLogin) {
            Motd.showMotd(event.getPlayer());
        }
    }
}