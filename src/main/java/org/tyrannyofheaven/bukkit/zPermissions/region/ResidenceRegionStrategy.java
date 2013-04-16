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
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHLoggingUtils;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

/**
 * RegionStrategy implementation for Residence.
 *
 * @author zerothangel
 */
public class ResidenceRegionStrategy implements RegionStrategy, Listener {

    private static final String RM_PLUGIN_NAME = "Residence";

    private final Plugin plugin;

    private boolean enabled;

    public ResidenceRegionStrategy(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return RM_PLUGIN_NAME;
    }

    @Override
    public boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin(RM_PLUGIN_NAME) != null;
    }

    @Override
    public void init() {
        detectResidencePlugin();
        if (!isEnabled()) {
            // Not yet loaded, listen for its enable event
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void shutdown() {
        enabled = false;
    }

    @Override
    public Set<String> getRegions(Location location) {
        ClaimedResidence res = Residence.getResidenceManager().getByLoc(location);
        if (res != null) {
            return Collections.singleton(res.getName().toLowerCase());
        }
        return Collections.emptySet();
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!isEnabled() && RM_PLUGIN_NAME.equals(event.getPlugin().getName())) {
            detectResidencePlugin();
            if (isEnabled())
                ToHLoggingUtils.log(plugin, "%s region support enabled.", getName());
        }
    }

    private void detectResidencePlugin() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(RM_PLUGIN_NAME);
        enabled = plugin != null && plugin.isEnabled();
        // Nothing else to do
    }

}
