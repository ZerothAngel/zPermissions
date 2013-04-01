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
package org.tyrannyofheaven.bukkit.zPermissions;

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.debug;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;

class ExpirationRefreshHandler implements Runnable {

    private static final Comparator<Membership> MEMBERSHIP_EXPIRATION_COMPARATOR = new Comparator<Membership>() {
        @Override
        public int compare(Membership a, Membership b) {
            return a.getExpiration().compareTo(b.getExpiration());
        }
    };

    private static final long FUDGE = 1000L;

    private final ZPermissionsPlugin plugin;

    private final Queue<Membership> membershipQueue = new PriorityQueue<Membership>(11, MEMBERSHIP_EXPIRATION_COMPARATOR);

    private final ScheduledExecutorService executorService;

    private ScheduledFuture<?> scheduledFuture;

    ExpirationRefreshHandler(ZPermissionsPlugin plugin) {
        this.plugin = plugin;
        
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    void rescan() {
        membershipQueue.clear();
        Date now = new Date();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Membership membership : plugin.getDao().getGroups(player.getName())) {
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
    public void run() {
        Set<String> toRefresh = new LinkedHashSet<String>();

        // Gather up memberships that have already expired
        Date now = new Date();
        Membership next = membershipQueue.peek();
        while (next != null && !next.getExpiration().after(now)) {
            membershipQueue.remove();

            toRefresh.add(next.getMember());

            now = new Date();
            next = membershipQueue.peek();
        }

        debug(plugin, "Refreshing expired players: %s", toRefresh);
        plugin.refreshPlayers(toRefresh);

        // Cancel previous task
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }

        // Schedule new task
        if (next != null) {
            now = new Date();
            long delay = next.getExpiration().getTime() - now.getTime();
            delay += FUDGE;

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
            }, delay, TimeUnit.MILLISECONDS);
        }
        else
            debug(plugin, "No future expirations");
    }

    void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
        }
        executorService.shutdownNow();
    }

}
