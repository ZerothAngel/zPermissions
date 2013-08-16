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

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Generic strategy for region support. Only requirement from region plugins is
 * being able to return a set of region names that contain a given location.
 * 
 * @author zerothangel
 */
public interface RegionStrategy {

    // General / detection

    /**
     * Returns the name of the region manager.
     * 
     * @return the name of the region manager
     */
    public String getName();

    /**
     * Returns whether or not this region plugin is present.
     * 
     * @return true if present. In which case this strategy will be used as the
     *     main RegionStrategy and {@link #init()} will be called.
     */
    public boolean isPresent();

    // Lifecycle

    /**
     * Attempt to initialize the region manager. Initialization may be synchronous or
     * happen later (e.g. PluginEnableEvent). After initialization, {@link #isEnabled()}
     * should always return true.
     */
    public void init();

    /**
     * Returns whether this RegionStrategy is ready.
     * 
     * @return true if ready to receive calls to {@link #getRegions(Location)}.
     */
    public boolean isEnabled();

    /**
     * Do any cleanup. Once this method completes, {@link #isEnabled()} should return false.
     * Will be called regardless of {@link #isEnabled()} state, but only if {@link #init()} was called.
     */
    public void shutdown();

    // Service

    /**
     * Retrieve the name of all regions that contain the given ocation.
     * 
     * @param location the location
     * @param player the player in question (for supplemental info)
     * @return name of containing regions or empty set. Never null. Region names
     *     must be in all lowercase.
     */
    public Set<String> getRegions(Location location, Player player);

}
