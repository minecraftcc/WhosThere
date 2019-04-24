package com.sleaker.WhosThere;

import me.lucko.luckperms.api.*;
import me.lucko.luckperms.api.caching.MetaData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LuckPermsManager {
    private static LuckPermsApi api = WhosThere.luckapi;

    public static boolean isPlayerInGroup(Player player, String group) {
        return player.hasPermission("group." + group);
    }

    private static User loadUser(Player player) {
        if (!player.isOnline()) throw new IllegalStateException("Player is offline");
        return api.getUserManager().getUser(player.getUniqueId());
    }

    private static MetaData getUserMeta(User user, Player player) {
        Contexts contexts = api.getContextManager().getApplicableContexts(player);
        return user.getCachedData().getMetaData(contexts);
    }

    private static MetaData getGroupMeta(Group group) {
        return group.getCachedData().getMetaData(Contexts.global());
    }

    public static String getUserPrefix(Player player) {
        User user = loadUser(player);
        MetaData metaData = getUserMeta(user, player);
        return metaData.getPrefix();
    }

    public static String getUserSuffix(Player player) {
        User user = loadUser(player);
        MetaData metaData = getUserMeta(user, player);
        return metaData.getSuffix();
    }

    public static void setUserPrefix(Player player, String prefix) {
        User user = loadUser(player);
        Node node;
        if (prefix == null) {
            String pre = getUserPrefix(player);
            node = api.getNodeFactory().makePrefixNode(99, pre).build();
            user.unsetPermission(node);
        } else {
            node = api.getNodeFactory().makePrefixNode(99, prefix).build();
            user.setPermission(node);
        }
        api.getUserManager().saveUser(user);
    }

    public static String getGroupPrefix(String grp) {
        Group group = api.getGroupManager().getGroup(grp);
        if (group == null) return "";
        MetaData metaData = getGroupMeta(group);
        return metaData.getPrefix();
    }

    public static String getGroupSuffix(String grp) {
        Group group = api.getGroupManager().getGroup(grp);
        if (group == null) return "";
        MetaData metaData = getGroupMeta(group);
        return metaData.getSuffix();
    }

    public static List<String> getPlayerGroups(Player player) {
        User user = loadUser(player);
        List<String> groups = new ArrayList<>(user.getAllNodes().stream()
                .filter(Node::isGroupNode)
                .map(Node::getGroupName)
                .collect(Collectors.toSet()));
        Collections.sort(groups, Collections.reverseOrder());
        return groups;
    }
}
