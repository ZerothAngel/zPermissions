/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
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

import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;

/**
 * Holder/parser for world-specific permissions as specified on the command-line.
 * Permissions with no world specifier assume the world is null (i.e. global
 * permission). Permissions specific to a world should be "&lt;world>:&lt;permission>"
 * 
 * @author zerothangel
 */
public class QualifiedPermission {

    private final String region;

    private final String world;
    
    private final String permission;

    public QualifiedPermission(String qualifiedPermission) {
        // Pull out region, if present
        String[] parts = qualifiedPermission.split("/", 2);
        if (parts.length == 1) {
            // No region
            region = null;
        }
        else {
            region = parts[0];
            qualifiedPermission = parts[1];
        }

        // Break up into world/permission, as appropriate
        parts = qualifiedPermission.split(":", 2);
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

    public QualifiedPermission(String region, String world, String permission) {
        if (!hasText(region))
            region = null;
        if (!hasText(world))
            world = null;
        if (!hasText(permission))
            throw new IllegalArgumentException("permission must have a value");
        
        this.region = region;
        this.world = world;
        this.permission = permission;
    }

    /**
     * Return the region if this is a region-specific permission
     * @return
     */
    public String getRegion() {
        return region;
    }

    /**
     * Return the name of the world if this is a world-specific permission.
     * 
     * @return the name of the world or null if global
     */
    public String getWorld() {
        return world;
    }

    /**
     * Return the permission.
     * 
     * @return the permission
     */
    public String getPermission() {
        return permission;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s",
                (getRegion() == null ? "" : getRegion() + "/"),
                (getWorld() == null ? "" : getWorld() + ":"),
                getPermission());
    }

}
