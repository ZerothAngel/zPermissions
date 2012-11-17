/*
 * Copyright 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

/**
 * Collection of static utils, constants, etc.
 * 
 * @author zerothangel
 */
public class Utils {

    public final static Comparator<PermissionEntity> PERMISSION_ENTITY_ALPHA_COMPARATOR = new Comparator<PermissionEntity>() {
        @Override
        public int compare(PermissionEntity a, PermissionEntity b) {
            return a.getDisplayName().compareTo(b.getDisplayName());
        }
    };

    public final static Comparator<Entry> ENTRY_COMPARATOR = new Comparator<Entry>() {
        @Override
        public int compare(Entry a, Entry b) {
            if (a.getRegion() != null && b.getRegion() == null)
                return 1;
            else if (a.getRegion() == null && b.getRegion() != null)
                return -1;
            else if (a.getRegion() != null && b.getRegion() != null) {
                int regions = a.getRegion().getName().compareTo(b.getRegion().getName());
                if (regions != 0) return regions;
            }

            if (a.getWorld() != null && b.getWorld() == null)
                return 1;
            else if (a.getWorld() == null && b.getWorld() != null)
                return -1;
            else if (a.getWorld() != null && b.getWorld() != null) {
                int worlds = a.getWorld().getName().compareTo(b.getWorld().getName());
                if (worlds != 0) return worlds;
            }

            return a.getPermission().compareTo(b.getPermission());
        }
    };

    private final static Comparator<PermissionInfo> PERMISSION_INFO_COMPARATOR = new Comparator<PermissionInfo>() {
        @Override
        public int compare(PermissionInfo a, PermissionInfo b) {
            return a.getPermission().compareTo(b.getPermission());
        }
    };

    public static List<Entry> sortPermissions(Collection<Entry> entries) {
        List<Entry> result = new ArrayList<Entry>(entries);
        Collections.sort(result, ENTRY_COMPARATOR);
        return result;
    }

    public static void displayPermissions(ZPermissionsPlugin plugin, CommandSender sender, Map<String, Boolean> permissions, String filter) {
        List<PermissionInfo> permList = new ArrayList<PermissionInfo>(permissions.size());
        for (Map.Entry<String, Boolean> me : permissions.entrySet()) {
            permList.add(new PermissionInfo(me.getKey(), me.getValue(), null));
        }
        displayPermissions(plugin, sender, permList, filter, false);
    }

    public static void displayPermissions(ZPermissionsPlugin plugin, CommandSender sender, List<PermissionInfo> permissions, String filter, boolean verbose) {
        // Sort for display
        permissions = new ArrayList<PermissionInfo>(permissions); // make copy
        Collections.sort(permissions, PERMISSION_INFO_COMPARATOR);

        // Convert to lines and filter
        List<String> lines = new ArrayList<String>(permissions.size());
        if (filter != null) {
            filter = filter.toLowerCase().trim();
            if (filter.isEmpty())
                filter = null;
        }
        for (PermissionInfo pi : permissions) {
            String key = pi.getPermission();
            if (filter != null && !key.contains(filter)) continue;
            String source;
            if (verbose) {
                source = pi.getSource() != null ? (ChatColor.RED + " [" + pi.getSource() + "]") : "";
            }
            else {
                boolean notMine = pi.getSource() != null &&
                        !plugin.getName().equals(pi.getSource());
                source = notMine? (ChatColor.RED + " *") : "";
            }
            lines.add(String.format(colorize("{DARK_GREEN}- {GOLD}%s{DARK_GREEN}: {GREEN}%s%s"), key, pi.getValue(), source));
        }

        if (lines.isEmpty()) {
            sendMessage(sender, colorize("{RED}No %spermissions found."), filter == null ? "" : "matching ");
        }
        else {
            ToHMessageUtils.displayLines(plugin, sender, lines);
        }
    }

    public static class PermissionInfo {
        
        private final String permission;
        
        private final boolean value;
        
        private final String source;
        
        public PermissionInfo(String permission, boolean value, String source) {
            if (!ToHStringUtils.hasText(permission))
                throw new IllegalArgumentException("permission must have a value");
            this.permission = permission.toLowerCase();
            this.value = value;
            this.source = source;
        }

        public String getPermission() {
            return permission;
        }

        public boolean getValue() {
            return value;
        }

        public String getSource() {
            return source;
        }

    }

}
