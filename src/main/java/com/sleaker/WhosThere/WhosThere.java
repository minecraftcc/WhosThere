package com.sleaker.WhosThere;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LuckPermsApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class WhosThere extends JavaPlugin {
    public static Plugin plugin;
    public static LuckPermsApi luckapi;
    public static List<String> players = new ArrayList<>();
    private Logger log = Logger.getLogger("Minecraft");
    private String plugName;
    private boolean usePrefix = true;
    private boolean useColorOption = false;
    private boolean displayOnLogin = false;
    private boolean prefixTabName = true;
    private boolean colorOptionTabName = false;
    private String colorOption = "namecolor";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM, dd - HH:mm");
    private static final int CHARS_PER_LINE = 52;
    private static final int LINES_PER_PAGE = 7;
    private static final String LINE_BREAK = "%LB%";

    public void onDisable() {
        this.log.info(this.plugName + " Disabled");
    }

    public void onEnable() {
        plugin = this;
        refreshInactive();
        PluginDescriptionFile pdfFile = this.getDescription();
        this.plugName = "[" + pdfFile.getName() + "]";

        //Check for LuckPerms
        PluginManager pm = Bukkit.getPluginManager();
        if (!pm.getPlugin("LuckPerms").isEnabled()) {
            pm.disablePlugin(this);
        } else {
            luckapi = LuckPerms.getApi();
        }

        ConfigManager.configExistence();
        this.setupConfiguration();

        this.getServer().getPluginManager().registerEvents(new WhoPlayerListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        this.log.info(this.plugName + " - " + pdfFile.getVersion() + " by Sleaker is enabled!");
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (command.getName().equalsIgnoreCase("who")) {
            Player player;
            if (sender instanceof Player && !this.has(player = (Player)sender, "whosthere.who")) {
                player.sendMessage("You don't have permission to do that.");
                return true;
            }
            this.whoCommand(sender, args);
            return true;
        }
        if (command.getName().equalsIgnoreCase("whois")) {
            if (sender instanceof Player && !this.has((Player)sender, "whosthere.whois")) {
                sender.sendMessage("You don't have permission to do that.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage("You must supply a username to get information about");
                return true;
            }
            this.whois(sender, args);
            return true;
        }
        if (command.getName().equalsIgnoreCase("whoall")) {
            if (sender instanceof Player && !this.has((Player)sender, "whosthere.whois")) {
                sender.sendMessage("You don't have permission to do that.");
                return true;
            }
            if (args.length > 1) {
                sender.sendMessage("Too many arguments!");
                return true;
            }
            this.whoAll(sender, args);
            return true;
        }
        if (command.getName().equalsIgnoreCase("findwho")) {
            if (sender instanceof Player && !this.has((Player)sender, "whosthere.find")) {
                sender.sendMessage("You don't have permission to do that.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage("You must supply a search string.");
                return true;
            }
            this.findCommand(sender, args);
            return true;
        }
        if (command.getName().equalsIgnoreCase("inactive")) {
            if (sender instanceof Player && !this.has((Player)sender, "whosthere.inactive")) {
                sender.sendMessage("You don't have permission to do that.");
                return true;
            }
            if (args.length > 1) {
                sender.sendMessage("Too many args!");
                return true;
            }
            refreshInactive();
            if (ConfigManager.inactivityNotify()) {
                for (String play : WhosThere.players) {
                    int days = WhosThere.getDaysOffline(play);
                    if (days > 59) {
                        sender.sendMessage(ChatColor.RED + "Inactive: " + ChatColor.AQUA + play);
                    }
                }
            }
        }
        return false;
    }

    private void refreshInactive() {
        if (ConfigManager.inactivityNotify()) {
            players.clear();

            for (String rank : ConfigManager.getRanks()) {
                Group group;
                try {
                    group = luckapi.getGroupManager().getGroup(rank);
                } catch (NullPointerException e) {
                    log.info("[WhosThere] Group " + rank + " not found!");
                    return;
                }
                CompletableFuture<List<HeldPermission<UUID>>> cf = luckapi.getUserManager().getWithPermission("group." + group.getName());
                List<HeldPermission<UUID>> users = cf.join().stream()
                        .filter(HeldPermission::getValue)
                        .collect(Collectors.toList());
                if (!users.isEmpty()) {
                    for (HeldPermission<UUID> user : users) {
                        String username = luckapi.getUserManager().getUser(user.getHolder()).getName();
                        if (!players.contains(username)) {
                            players.add(username);
                        }
                    }
                }
            }
        }
    }

    private void setupConfiguration() {
        FileConfiguration config = this.getConfig();
        config.options().copyDefaults(true);
        this.saveConfig();
        this.usePrefix = ConfigManager.usePrefix();
        this.useColorOption = ConfigManager.useColorOption();
        this.colorOption = ConfigManager.colorOptionName();
        this.displayOnLogin = ConfigManager.displayOnLogin();
        this.prefixTabName = ConfigManager.prefixTabName();
        this.colorOptionTabName = ConfigManager.colorOptionTabName();
    }

    private boolean has(Player player, String permission) {
        return player.hasPermission(permission);
    }

    private String prefix(Player player) {
        return LuckPermsManager.getUserPrefix(player);
    }

    private void whois(CommandSender sender, String[] args) {
        Player p = null;
        if (sender instanceof Player && args.length == 0) {
            p = (Player)sender;
        }
        for (Player pl2 : Bukkit.getServer().getOnlinePlayers()) {
            if (!pl2.getName().contains(args[0])) continue;
            p = pl2;
            break;
        }
        if (p == null) {
            for (Player pl2 : Bukkit.getServer().getOnlinePlayers()) {
                if (!pl2.getDisplayName().toLowerCase().contains(args[0])) continue;
                p = pl2;
                break;
            }
        }
        if (p != null) {
            if (sender instanceof Player && !((Player)sender).canSee(p)) {
                if (!this.checkOfflinePlayer(args[0], sender)) {
                    sender.sendMessage("No player with name " + args[0] + " was found on the server");
                }
                return;
            }
            sender.sendMessage(this.replaceColors("&a----  " + this.colorize(p) + "&a----"));
            if (sender instanceof Player && !this.has((Player)sender, "whosthere.admin")) {
                return;
            }
            sender.sendMessage(this.replaceColors("&aUUID: &d" + p.getUniqueId()));
            Location pLoc = p.getLocation();
            sender.sendMessage(this.replaceColors("&aLoc: &d" + pLoc.getBlockX() + "&a, &d" + pLoc.getBlockY() + "&a, &d" + pLoc.getBlockZ() + "&a on: &d" + pLoc.getWorld().getName()));
            long temp = p.getFirstPlayed();
            sender.sendMessage(this.replaceColors("&aFirst Online: &d" + (temp != 0 ? this.dateFormat.format(new Date(temp)) : " unknown")));
            sender.sendMessage(this.replaceColors("&aIP: &d" + p.getAddress().getAddress().getHostAddress()));
        } else if (!this.checkOfflinePlayer(args[0], sender)) {
            sender.sendMessage("No player with name " + args[0] + " was found on the server");
        }
    }

    private boolean checkOfflinePlayer(String playerName, CommandSender sender) {
        long temp;
        OfflinePlayer op = Bukkit.getServer().getOfflinePlayer(playerName);
        if (op == null || !op.hasPlayedBefore()) {
            return false;
        }
        sender.sendMessage(this.replaceColors("&aFirst Online: &d" + ((temp = op.getFirstPlayed()) != 0 ? this.dateFormat.format(new Date(temp)) : " unknown")));
        temp = op.getLastPlayed();
        sender.sendMessage(this.replaceColors("&aLast Online: &d" + (temp != 0 ? this.dateFormat.format(new Date(temp)) : " unknown")));
        return true;
    }

    public static int getDaysOffline(String playerName) {
        OfflinePlayer op = Bukkit.getServer().getOfflinePlayer(playerName);
        if (op == null || !op.hasPlayedBefore()) {
            return 60;
        }
        long last = op.getLastPlayed();
        long current = System.currentTimeMillis();
        Date lastPlay = new Date(last);
        Date currentPlay = new Date(current);
        return (int)((currentPlay.getTime() - lastPlay.getTime()) / (1000 * 60 * 60 * 24));
    }

    private void findCommand(CommandSender sender, String[] args) {
        int page = 0;
        if (args.length > 1) {
            try {
                Integer val = Integer.parseInt(args[1]);
                page = val - 1;
            }
            catch (NumberFormatException e) {
                // empty catch block
            }
        }
        String playerList = "";
        int i = 0;
        int remainingChars = CHARS_PER_LINE;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && !((Player)sender).canSee(player) || !player.getName().toLowerCase().contains(args[0].toLowerCase())) continue;
            if (remainingChars - player.getName().length() < 0) {
                playerList = playerList + LINE_BREAK;
                remainingChars = CHARS_PER_LINE;
            }
            playerList = playerList + ChatColor.WHITE;
            if (remainingChars != CHARS_PER_LINE) {
                playerList = playerList + "  ";
            }
            playerList = playerList + this.colorize(player);
            remainingChars -= player.getName().length() + 2;
            ++i;
        }
        List<String> lines = Arrays.asList(playerList.split(LINE_BREAK));
        int totalPages = lines.size() % LINES_PER_PAGE;
        if (page >= totalPages || page < 0) {
            page = 0;
        }
        if (i == 0) {
            sender.sendMessage("No players found with that name.");
        } else {
            String title = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players matching your criteria. Showing page " + ChatColor.BLUE + (page + 1) + "/" + (totalPages + 1);
            this.sendWrappedText(sender, title, lines, page);
        }
    }

    private void whoCommand(CommandSender sender, String[] args) {
        Integer val;
        World world = null;
        int page = 0;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("staff")) {
                this.whoStaff(sender, args);
                return;
            }
            world = this.getServer().getWorld(args[0]);
            if (world == null) {
                try {
                    val = Integer.parseInt(args[0]);
                    page = val - 1;
                }
                catch (NumberFormatException e) {
                    // empty catch block
                }
            }
        }
        if (args.length > 1) {
            try {
                val = Integer.parseInt(args[1]);
                page = val - 1;
            }
            catch (NumberFormatException e) {
                // empty catch block
            }
        }
        String playerList = "";
        int i = 0;
        int remainingChars = CHARS_PER_LINE;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && !((Player)sender).canSee(player) || (world != null || args.length != 0) && (world == null || !player.getWorld().equals(world))) continue;
            if (remainingChars - player.getName().length() < 0) {
                playerList = playerList + LINE_BREAK;
                remainingChars = CHARS_PER_LINE;
            }
            playerList = playerList + ChatColor.WHITE;
            if (remainingChars != CHARS_PER_LINE) {
                playerList = playerList + "  ";
            }
            playerList = playerList + this.colorize(player);
            remainingChars -= player.getName().length() + 2;
            ++i;
        }
        List<String> lines = Arrays.asList(playerList.split(LINE_BREAK));
        int totalPages = lines.size() % LINES_PER_PAGE;
        if (page >= totalPages || page < 0) {
            page = 0;
        }
        if (i == 0 && world != null) {
            sender.sendMessage("No players were found on " + world.getName());
        } else if (world != null) {
            String title = ChatColor.WHITE + "Found " + ChatColor.BLUE + i + ChatColor.WHITE + " players on " + world.getName() + ". Showing page " + ChatColor.BLUE + (page + 1) + "/" + (totalPages + 1);
            this.sendWrappedText(sender, title, lines, page);
        } else {
            String title = ChatColor.WHITE + "There " + (i > 1 ? "are " : "is ") + ChatColor.BLUE + i + "/" + this.getServer().getMaxPlayers() + ChatColor.WHITE + " players online. Showing page " + ChatColor.BLUE + (page + 1) + "/" + (totalPages + 1);
            this.sendWrappedText(sender, title, lines, page);
        }
    }

    private void whoAll(CommandSender sender, String[] args) {
        Map<World, ArrayList<Player>> players = new HashMap<>();
        sender.sendMessage(ChatColor.WHITE + "--- Players Per World ---");
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && !((Player)sender).canSee(player)) continue;
            World w = player.getWorld();
            players.putIfAbsent(w, new ArrayList<>());
            players.get(w).add(player);
        }

        for (World w : players.keySet()) {
            ArrayList<Player> ps = players.get(w);
            sender.sendMessage(ChatColor.WHITE + w.getName());
            for (Player p : ps) {
                sender.sendMessage(ChatColor.GRAY + "- " + this.colorize(p));
            }
        }
    }

    private void whoStaff(CommandSender sender, String[] args) {
        String playerList = "";
        int i = 0;
        int remainingChars = CHARS_PER_LINE;
        int page = 0;
        if (args.length > 1) {
            try {
                Integer val = Integer.parseInt(args[1]);
                page = val - 1;
            }
            catch (NumberFormatException e) {
                // empty catch block
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && !((Player)sender).canSee(player) || !player.hasPermission("whosthere.staff")) continue;
            if (remainingChars - player.getName().length() < 0) {
                playerList = playerList + LINE_BREAK;
                remainingChars = CHARS_PER_LINE;
            }
            if (remainingChars != CHARS_PER_LINE) {
                playerList = playerList + "  ";
            }
            playerList = playerList + this.colorize(player);
            remainingChars -= player.getName().length() + 2;
            playerList = playerList + ChatColor.WHITE;
            ++i;
        }
        List<String> lines = Arrays.asList(playerList.split(LINE_BREAK));
        int totalPages = lines.size() % LINES_PER_PAGE;
        if (page >= totalPages || page < 0) {
            page = 0;
        }
        if (i == 0) {
            sender.sendMessage("No staff are currently online!");
        } else {
            String title = ChatColor.WHITE + "There " + (i > 1 ? "are " : "is ") + ChatColor.BLUE + i + ChatColor.WHITE + " staff online. Showing page " + ChatColor.BLUE + (page + 1) + "/" + (totalPages + 1);
            this.sendWrappedText(sender, title, lines, page);
        }
    }

    private String colorize(Player p) {
        String message = "";
        if (this.usePrefix) {
            message = message + this.prefix(p);
        }
        message = message + p.getName();
        return this.replaceColors(message);
    }

    private String colorizeTabName(Player p) {
        String message = "";
        if (this.prefixTabName) {
            message = message + this.prefix(p);
        }
        message = message + p.getName();
        return this.replaceColors(message);
    }

    private String replaceColors(String message) {
        return message.replaceAll("(?i)&([a-fk-o0-9])", "\u00a7$1");
    }

    private void sendWrappedText(CommandSender sender, String header, List<String> lines, int pageNumber) {
        sender.sendMessage(header);
        int end = (pageNumber + 1) * LINES_PER_PAGE;
        if (end > lines.size()) {
            end = lines.size();
        }
        for (int i = pageNumber * LINES_PER_PAGE; i < end; ++i) {
            sender.sendMessage(lines.get(i));
        }
    }

    private class WhoPlayerListener implements Listener {
        WhosThere plugin;

        private WhoPlayerListener(WhosThere plugin) {
            this.plugin = plugin;
        }

        @EventHandler(priority=EventPriority.MONITOR)
        public void onPlayerJoin(PlayerJoinEvent event) {
            final Player player = event.getPlayer();
            if (WhosThere.this.displayOnLogin) {
                this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable(){

                    public void run() {
                        WhoPlayerListener.this.plugin.getServer().getPluginCommand("who").execute(player, "who", new String[0]);
                    }
                }, 1);
            }
            if (WhosThere.this.prefixTabName || WhosThere.this.colorOptionTabName) {
                String listName = WhosThere.this.colorizeTabName(player);
                if (listName.length() > 16) {
                    listName = listName.substring(0, 15);
                }
                player.setPlayerListName(listName);
            }
        }

    }

    public class PlayerComparator implements Comparator<Player> {
        public int compare(Player p1, Player p2) {
            return p1.getName().compareTo(p2.getName());
        }
    }

}