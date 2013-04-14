/*
 * Copyright 2013 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
package org.tyrannyofheaven.bukkit.zPermissions.region;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * RegionStrategy implementation for WorldGuard.
 * 
 * @author zerothangel
 */
public class WorldGuardRegionStrategy implements RegionStrategy {

    private WorldGuardPlugin worldGuardPlugin;

    @Override
    public String getName() {
        return "WorldGuard";
    }

    @Override
    public boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    @Override
    public void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (plugin instanceof WorldGuardPlugin) {
            worldGuardPlugin = (WorldGuardPlugin)plugin;
        }
        else {
            worldGuardPlugin = null;
        }
    }

    @Override
    public boolean isEnabled() {
        return worldGuardPlugin != null;
    }

    @Override
    public void shutdown() {
        worldGuardPlugin = null;
    }

    @Override
    public Set<String> getRegions(Location location) {
        if (isEnabled()) {
            RegionManager rm = worldGuardPlugin.getRegionManager(location.getWorld());
            if (rm != null) {
                ApplicableRegionSet ars = rm.getApplicableRegions(location);

                Set<String> result = new HashSet<String>();
                for (ProtectedRegion pr : ars) {
                    // Ignore global region
                    if (!"__global__".equals(pr.getId())) // NB: Hardcoded and not available as constant in WorldGuard
                        result.add(pr.getId().toLowerCase());
                }
                return result;
            }
        }
        return Collections.emptySet();
    }

}
