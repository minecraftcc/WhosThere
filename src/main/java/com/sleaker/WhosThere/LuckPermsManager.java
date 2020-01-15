package com.sleaker.WhosThere;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.query.QueryOptions;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class LuckPermsManager {
    private static LuckPerms api = WhosThere.luckapi;

    public static boolean isPlayerInGroup(Player player, String group) {
        return player.hasPermission("group." + group);
    }

    private static User loadUser(Player player) {
        if (!player.isOnline()) throw new IllegalStateException("Player is offline");
        return api.getUserManager().getUser(player.getUniqueId());
    }

    private static CachedMetaData getUserMeta(User user, Player player) {
        QueryOptions contexts = api.getContextManager().getQueryOptions(player);
        return user.getCachedData().getMetaData(contexts);
    }

    private static CachedMetaData getGroupMeta(Group group) {
        QueryOptions contexts = api.getContextManager().getStaticQueryOptions();
        return group.getCachedData().getMetaData(contexts);
    }

    public static String getUserPrefix(Player player) {
        User user = loadUser(player);
        CachedMetaData metaData = getUserMeta(user, player);
        return metaData.getPrefix();
    }

    public static String getUserSuffix(Player player) {
        User user = loadUser(player);
        CachedMetaData metaData = getUserMeta(user, player);
        return metaData.getSuffix();
    }

    public static void setUserPrefix(Player player, String prefix) {
        User user = loadUser(player);
        PrefixNode node;
        if (prefix == null) {
            String pre = getUserPrefix(player);
            node = PrefixNode.builder(pre, 99).build();
            user.data().remove(node);
        } else {
            node = PrefixNode.builder(prefix, 99).build();
            user.data().add(node);
        }
        api.getUserManager().saveUser(user);
    }

    public static String getGroupPrefix(String grp) {
        Group group = api.getGroupManager().getGroup(grp);
        if (group == null) return "";
        CachedMetaData metaData = getGroupMeta(group);
        return metaData.getPrefix();
    }

    public static String getGroupSuffix(String grp) {
        Group group = api.getGroupManager().getGroup(grp);
        if (group == null) return "";
        CachedMetaData metaData = getGroupMeta(group);
        return metaData.getSuffix();
    }

    public static List<String> getPlayerGroups(Player player) {
        User user = loadUser(player);
        List<String> list = user.resolveInheritedNodes(QueryOptions.nonContextual()).stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .map(InheritanceNode::getGroupName).collect(Collectors.toList());
        return list;
    }

    public static String getPlayerMainGroup(Player player) {
        User user = loadUser(player);
        return user.getPrimaryGroup();
    }
}
