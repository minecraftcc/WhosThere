package com.sleaker.WhosThere;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import static com.sleaker.WhosThere.ConfigManager.daysUntilInactive;

/**
 * Created by bmlzootown on 5/24/2017.
 */
public class PlayerJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (p.hasPermission("whosthere.inactivity")) {
            for (String player : WhosThere.players) {
                int days = WhosThere.getDaysOffline(player);
                if (days > daysUntilInactive()) {
                    p.sendMessage(ChatColor.RED + "Inactive: " + ChatColor.AQUA + player);
                }
            }
        }
    }

}
