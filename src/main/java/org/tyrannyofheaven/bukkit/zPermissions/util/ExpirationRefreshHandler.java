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
package org.tyrannyofheaven.bukkit.zPermissions.util;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.broadcast;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;

public class ExpirationRefreshHandler implements Runnable {

    private static final Comparator<Membership> MEMBERSHIP_EXPIRATION_COMPARATOR = new Comparator<Membership>() {
        @Override
        public int compare(Membership a, Membership b) {
            return a.getExpiration().compareTo(b.getExpiration());
        }
    };

    private static final long FUDGE = 1000L;

    private final ZPermissionsCore core;

    private final StorageStrategy storageStrategy;

    private final Plugin plugin;

    private final ScheduledExecutorService executorService;

    private final Queue<Membership> membershipQueue = new PriorityQueue<>(11, MEMBERSHIP_EXPIRATION_COMPARATOR); // synchronized on this

    private ScheduledFuture<?> scheduledFuture; // synchronized on this

    public ExpirationRefreshHandler(ZPermissionsCore core, StorageStrategy storageStrategy, Plugin plugin) {
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.plugin = plugin;
        
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    public synchronized void rescan() {
        membershipQueue.clear();
        Date now = new Date();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Membership membership : storageStrategy.getDao().getGroups(player.getUniqueId())) {
                if (membership.getExpiration() != null && membership.getExpiration().after(now)) {
                    membershipQueue.add(membership);
                }
            }
        }

        debug(plugin, "Potential future expirations: %s", membershipQueue);

        // Queue up task
        run();
    }

    @Override
    public synchronized void run() {
        Set<UUID> toRefresh = new LinkedHashSet<>();
        final Set<Membership> expired = new LinkedHashSet<>();

        // Gather up memberships that have already expired
        Date now = new Date();
        Membership next = membershipQueue.peek();
        while (next != null && !next.getExpiration().after(now)) {
            membershipQueue.remove();

            toRefresh.add(next.getUuid());
            expired.add(next);

            now = new Date();
            next = membershipQueue.peek();
        }

        debug(plugin, "Refreshing expired players: %s", toRefresh);
        // NB Metadata cache for offline players not invalidated.
        // This might become a problem. But nothing can be done unless we
        // run a timer for each and every membership, online or offline.
        core.refreshPlayers(toRefresh);

        // Send notifications
        if (!expired.isEmpty()) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    for (Membership membership : expired) {
                        Player player = Bukkit.getPlayer(membership.getUuid());
                        if (player != null && player.hasPermission("zpermissions.notify.self.expiration")) {
                            sendMessage(player, colorize("{YELLOW}Your membership to {DARK_GREEN}%s{YELLOW} has expired."), membership.getGroup().getDisplayName());
                        }
                        broadcast(plugin, "zpermissions.notify.expiration",
                                "Player %s is no longer a member of %s",
                                membership.getDisplayName(), membership.getGroup().getDisplayName());
                    }
                }
            });
        }

        // Cancel previous task
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }

        // Schedule new task
        if (next != null) {
            now = new Date();
            long delay = next.getExpiration().getTime() - now.getTime();

            if (delay < 0L)
                delay = 0L; // Weird...

            debug(plugin, "Next expiration is %dms away", delay);

            final Runnable realThis = this;
            scheduledFuture = executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    debug(plugin, "Expiring...");
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, realThis);
                }
            }, delay + FUDGE, TimeUnit.MILLISECONDS);
        }
        else
            debug(plugin, "No future expirations");
    }

    public void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        executorService.shutdownNow();
    }

}
