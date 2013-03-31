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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.command.ParseException;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
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

    public static void displayPermissions(ZPermissionsPlugin plugin, CommandSender sender, List<String> header, Map<String, Boolean> permissions, String filter) {
        List<PermissionInfo> permList = new ArrayList<PermissionInfo>(permissions.size());
        for (Map.Entry<String, Boolean> me : permissions.entrySet()) {
            permList.add(new PermissionInfo(me.getKey(), me.getValue(), null));
        }
        displayPermissions(plugin, sender, header, permList, filter, false);
    }

    public static void displayPermissions(ZPermissionsPlugin plugin, CommandSender sender, List<String> header, List<PermissionInfo> permissions, String filter, boolean verbose) {
        if (header == null)
            header = Collections.emptyList();

        // Sort for display
        permissions = new ArrayList<PermissionInfo>(permissions); // make copy
        Collections.sort(permissions, PERMISSION_INFO_COMPARATOR);

        // Convert to lines and filter
        List<String> lines = new ArrayList<String>(header.size() + permissions.size());
        lines.addAll(header);
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

    public static List<String> toMembers(Collection<Membership> memberships) {
        List<String> result = new ArrayList<String>(memberships.size());
        for (Membership membership : memberships) {
            result.add(membership.getMember());
        }
        return result;
    }

    public static List<String> toGroupNames(Collection<Membership> memberships) {
        List<String> result = new ArrayList<String>(memberships.size());
        for (Membership membership : memberships) {
            result.add(membership.getGroup().getDisplayName());
        }
        return result;
    }

    public static List<Membership> filterExpired(Collection<Membership> memberships) {
        List<Membership> result = new ArrayList<Membership>(memberships.size());
        Date now = new Date();
        for (Membership membership : memberships) {
            if (membership.getExpiration() == null || membership.getExpiration().after(now))
                result.add(membership);
        }
        return result;
    }

    public static Date parseDurationTimestamp(String duration, String units) {
        if (!ToHStringUtils.hasText(duration))
            return null;
        
        duration = duration.trim().toUpperCase();

        Integer durationInt;
        try {
            durationInt = Integer.valueOf(duration);
        }
        catch (NumberFormatException e) {
            // Try ISO 8601 date
            try {
                Calendar cal = DatatypeConverter.parseDateTime(duration);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTime();
            }
            catch (IllegalArgumentException e2) {
                // One last try. Append :00
                // WHY U SO STRICT DatatypeConverter?!
                try {
                    Calendar cal = DatatypeConverter.parseDateTime(duration + ":00");
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal.getTime();
                }
                catch (IllegalArgumentException e3) {
                    throw new ParseException("Invalid value: duration/timestamp"); // NB Should match option name
                }
            }
        }

        if (durationInt < 1)
            throw new ParseException("Invalid value: duration/timestamp"); // NB Should match option name

        int unitsInt = Calendar.DAY_OF_MONTH;
        if (ToHStringUtils.hasText(units)) {
            units = units.trim().toLowerCase();

            if ("hours".equals(units) || "hour".equals(units) || "h".equals(units))
                unitsInt = Calendar.HOUR;
            else if ("days".equals(units) || "day".equals(units) || "d".equals(units))
                unitsInt = Calendar.DAY_OF_MONTH;
            else if ("months".equals(units) || "month".equals(units) || "m".equals(units))
                unitsInt = Calendar.MONTH;
            else if ("years".equals(units) || "year".equals(units) || "y".equals(units))
                unitsInt = Calendar.YEAR;
            else
                throw new ParseException("units must be hours, days, months, years");
        }
        
        Calendar cal = Calendar.getInstance();
        cal.add(unitsInt, durationInt);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // Suitable for user viewing (e.g. not dumps)
    public static String dateToString(Date date) {
        if (date == null)
            throw new IllegalArgumentException("date cannot be null");
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        String result = DatatypeConverter.printDateTime(cal);
        
        if (result.length() < 16)
            return result;
        else
            return result.substring(0, 16);
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
