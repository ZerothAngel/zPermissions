/*
 * Copyright 2013 Allan Saddi <allan@saddi.com>
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

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

/**
 * RegionStrategy implementation for Residence.
 *
 * @author asaddi
 */
public class ResidenceRegionStrategy implements RegionStrategy {

    private boolean enabled;

    @Override
    public String getName() {
        return "Residence";
    }

    @Override
    public boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("Residence") != null;
    }

    @Override
    public void init() {
        enabled = true;
        // Nothing else to do
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

}
