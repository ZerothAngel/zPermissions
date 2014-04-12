/*
 * Copyright 2014 Allan Saddi <allan@saddi.com>
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
package org.tyrannyofheaven.bukkit.zPermissions.uuid;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.ToHStringUtils.hasText;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.isBatchProcessing;
import static org.tyrannyofheaven.bukkit.zPermissions.uuid.UuidUtils.parseUuidDisplayName;

import java.util.UUID;
import java.util.concurrent.Executor;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CommandUuidResolver {

    private final Plugin plugin;

    private final UuidResolver uuidResolver;

    private final Executor executor;

    public CommandUuidResolver(Plugin plugin, UuidResolver uuidResolver, Executor executor) {
        this.plugin = plugin;
        this.uuidResolver = uuidResolver;
        this.executor = executor;
    }

    public void resolveUsername(CommandSender sender, String name, boolean group, boolean forceInline, CommandUuidResolverHandler handler) {
        if (sender == null)
            throw new IllegalArgumentException("sender cannot be null");
        if (handler == null)
            throw new IllegalArgumentException("handler cannot be null");

        if (group || name == null) {
            // Simple case: no need to resolve because it's a group or name is null, run inline
            handler.process(sender, name, null, group);
        }
        else {
            // See if it's UUID or UUID/DisplayName
            UuidDisplayName udn = parseUuidDisplayName(name);
            if (udn != null) {
                String displayName;
                OfflinePlayer player = Bukkit.getOfflinePlayer(udn.getUuid());
                if (player != null && player.getName() != null) {
                    // Use last known name
                    displayName = player.getName();
                }
                else {
                    // Default display name (either what was passed in or the UUID in string form)
                    displayName = hasText(udn.getDisplayName()) ? udn.getDisplayName() : udn.getUuid().toString();
                }
                handler.process(sender, displayName, udn.getUuid(), group);
            }
            else {
                // Is the named player online?
                Player player = Bukkit.getPlayerExact(name);
                if (player != null) {
                    // Simply run inline, no explicit lookup necessary
                    handler.process(sender, player.getName(), player.getUniqueId(), group);
                }
                else if (forceInline) {
                    // Lookup & run inline
                    udn = uuidResolver.resolve(name);
                    if (udn == null) {
                        fail(sender, name);
                    }
                    else {
                        handler.process(sender, udn.getDisplayName(), udn.getUuid(), group);
                    }
                }
                else {
                    // Resolve async
                    sendMessage(sender, colorize("{GRAY}(Resolving UUID...)"));
                    Runnable task = new UsernameResolverHandlerRunnable(plugin, uuidResolver, sender, name, group, handler);
                    // NB Bukkit#getOfflinePlayer(String) provides almost the same service
                    // However, it's not known whether it is fully thread-safe.
                    executor.execute(task);
                }
            }
        }
    }

    private static class UsernameResolverHandlerRunnable implements Runnable {

        private final Plugin plugin;
        
        private final UuidResolver uuidResolver;

        private final CommandSender sender;

        private final UUID senderUuid;

        private final String name;

        private final boolean group;

        private final CommandUuidResolverHandler handler;

        private UsernameResolverHandlerRunnable(Plugin plugin, UuidResolver uuidResolver, CommandSender sender, String name, boolean group, CommandUuidResolverHandler handler) {
            this.plugin = plugin;
            this.uuidResolver = uuidResolver;
            this.sender = sender instanceof Player ? null : sender;
            this.senderUuid = sender instanceof Player ? ((Player)sender).getUniqueId() : null;
            this.name = name;
            this.group = group;
            this.handler = handler;
        }

        private CommandSender getSender() {
            return sender;
        }

        @Override
        public void run() {
            // Perform lookup
            final UuidDisplayName udn = uuidResolver.resolve(name);

            // Run the rest in the main thread
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    // Re-lookup sender
                    CommandSender sender = getSender() != null ? getSender() : Bukkit.getPlayer(senderUuid);

                    // Only execute if sender is still around
                    if (sender != null) {
                        if (udn == null) {
                            fail(sender, name);
                        }
                        else {
                            handler.process(sender, udn.getDisplayName(), udn.getUuid(), group);
                        }
                    }
                }
            });
        }

    }

    public void resolveUsername(CommandSender sender, String name, boolean group, CommandUuidResolverHandler handler) {
        resolveUsername(sender, name, group, isBatchProcessing(), handler);
    }     

    private static void fail(CommandSender sender, String name) {
        sendMessage(sender, colorize("{RED}Failed to lookup UUID for {AQUA}%s"), name);
        // TODO After version 1.3, re-enable this abort
        // Leave it out for for 1.3 so initial UUID import is a bit more forgiving
//        abortBatchProcessing();
    }

}
