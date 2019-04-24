package com.sleaker.WhosThere;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by bmlzootown on 5/24/2017.
 */
public class ConfigManager {
    static Plugin pl = WhosThere.plugin;

    public static void configExistence() {
        File file = new File(pl.getDataFolder(), "config.yml");
        if (!file.exists()) {setConfigDefaults();}
    }

    public static void setConfigDefaults() {
        FileConfiguration config = pl.getConfig();
        List<String> ranks = Arrays.asList("noobmod", "moderator", "advancedmoderator");
        config.addDefault("use-prefix", true);
        config.addDefault("use-color-option", false);
        config.addDefault("display-on-login", false);
        config.addDefault("color-option-name", "InfoNode");
        config.addDefault("prefix-tab-name", true);
        config.addDefault("color-option-tab-name", false);
        config.addDefault("inactivity-notify", true);
        config.addDefault("days-until-inactive", 60);
        config.addDefault("ranks", ranks);
        config.options().copyDefaults(true);
        pl.saveConfig();
    }

    public static boolean usePrefix() { return pl.getConfig().getBoolean("use-prefix"); }

    public static boolean useColorOption() { return pl.getConfig().getBoolean("use-color-option"); }

    public static boolean displayOnLogin() { return pl.getConfig().getBoolean("display-on-login"); }

    public static String colorOptionName() { return pl.getConfig().getString("color-option-name"); }

    public static boolean prefixTabName() { return pl.getConfig().getBoolean("prefix-tab-name"); }

    public static boolean colorOptionTabName() { return pl.getConfig().getBoolean("color-option-tab-name"); }

    public static boolean inactivityNotify() { return pl.getConfig().getBoolean("inactivity-notify"); }

    public static Integer daysUntilInactive() { return pl.getConfig().getInt("days-until-inactive"); }

    public static List<String> getRanks() { return pl.getConfig().getStringList("ranks"); }

}
