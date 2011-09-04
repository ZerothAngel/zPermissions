package org.tyrannyofheaven.bukkit.zPermissions;

public class WorldPermission {

    private final String world;
    
    private final String permission;

    WorldPermission(String worldPermission) {
        String[] parts = worldPermission.split(":", 2);
        if (parts.length == 1) {
            // No world
            world = null;
            permission = parts[0];
        }
        else {
            world = parts[0];
            permission = parts[1];
        }
    }

    public String getWorld() {
        return world;
    }

    public String getPermission() {
        return permission;
    }

}
