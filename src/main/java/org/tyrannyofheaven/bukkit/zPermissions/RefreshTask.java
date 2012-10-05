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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.error;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.delimitedString;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Bukkit;

/**
 * Periodically calls {@link ZPermissionsPlugin#refreshPlayer(String)} on the
 * given queue of players.
 * 
 * @author zerothangel
 */
public class RefreshTask implements Runnable {

    private final ZPermissionsPlugin plugin;

    private final Queue<String> playersToRefresh = new LinkedList<String>();

    private int delay;

    private int taskId = -1;

    RefreshTask(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    void setDelay(int delay) {
        if (delay < 0)
            delay = 0;
        this.delay = delay;
    }

    void start(Collection<String> playerNames) {
        if (playerNames == null || playerNames.isEmpty())
            return; // Nothing to do

        // Build a set to maintain uniqueness
        Set<String> nextPlayersToRefresh = new LinkedHashSet<String>(playersToRefresh);
        
        // Remember who to refresh
        for (String playerName : playerNames) {
            // Canonicalize
            nextPlayersToRefresh.add(playerName.toLowerCase());
        }

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

    void stop() {
        if (taskId > -1) {
            warn(plugin, "RefreshTask cancelled prematurely! Remaining players: %s", delimitedString(", ", playersToRefresh));
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public void run() {
        taskId = -1;

        if (!playersToRefresh.isEmpty()) {
            String playerToRefresh = playersToRefresh.remove();

            // Refresh single player
            plugin.refreshPlayer(playerToRefresh);
        }
        
        // Schedule next player
        if (!playersToRefresh.isEmpty()) {
            scheduleTask();
        }
        else
            debug(plugin, "Done doing background refresh!");
    }

}
