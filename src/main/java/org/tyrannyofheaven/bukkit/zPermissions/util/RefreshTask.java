/*
 * Copyright 2012 Allan Saddi <allan@saddi.com>
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
package org.tyrannyofheaven.bukkit.zPermissions.util;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.error;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.zPermissions.RefreshCause;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;

/**
 * Periodically calls {@link ZPermissionsCore#refreshPlayer(UUID, RefreshCause)} on the
 * given queue of players.
 * 
 * @author asaddi
 */
public class RefreshTask implements Runnable {

    private final ZPermissionsCore core;

    private final Plugin plugin;

    private int delay;

    private final Queue<UUID> playersToRefresh = new LinkedList<>(); // synchronized on this

    private int taskId = -1; // synchronized on this

    public RefreshTask(ZPermissionsCore core, Plugin plugin) {
        this.core = core;
        this.plugin = plugin;
    }

    public void setDelay(int delay) {
        if (delay < 0)
            delay = 0;
        this.delay = delay;
    }

    public synchronized void start(Collection<UUID> playerUuids) {
        if (playerUuids == null || playerUuids.isEmpty())
            return; // Nothing to do

        // Build a set to maintain uniqueness
        Set<UUID> nextPlayersToRefresh = new LinkedHashSet<>(playersToRefresh);

        // Remember who to refresh
        nextPlayersToRefresh.addAll(playerUuids);

        // Replace queue with set
        playersToRefresh.clear();
        playersToRefresh.addAll(nextPlayersToRefresh);

        // Schedule task if not already scheduled
        if (taskId < 0) {
            debug(plugin, "Scheduling background refresh");
            scheduleTask();
        }
    }

    private void scheduleTask() {
        if ((taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, (long)delay)) < 0) {
            error(plugin, "Failed to schedule RefreshTask! Remaining players: %s", delimitedString(", ", playersToRefresh));
        }
    }

    public synchronized void stop() {
        if (taskId > -1) {
            warn(plugin, "RefreshTask cancelled prematurely! Remaining players: %s", delimitedString(", ", playersToRefresh));
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public synchronized void run() {
        taskId = -1;

        if (!playersToRefresh.isEmpty()) {
            UUID playerToRefresh = playersToRefresh.remove();

            // Refresh single player
            core.invalidateMetadataCache("ignored", playerToRefresh, false);
            core.refreshPlayer(playerToRefresh, RefreshCause.GROUP_CHANGE); // NB Assumes all who call start() are doing so for group- or server-wide changes
        }
        
        // Schedule next player
        if (!playersToRefresh.isEmpty()) {
            scheduleTask();
        }
        else
            debug(plugin, "Done doing background refresh!");
    }

}
