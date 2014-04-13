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
package org.tyrannyofheaven.bukkit.zPermissions.command;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;
import static org.tyrannyofheaven.bukkit.util.command.reader.CommandReader.abortBatchProcessing;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.command.Require;
import org.tyrannyofheaven.bukkit.util.command.Session;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallback;
import org.tyrannyofheaven.bukkit.util.transaction.TransactionCallbackWithoutResult;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolver;
import org.tyrannyofheaven.bukkit.util.uuid.CommandUuidResolverHandler;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsCore;
import org.tyrannyofheaven.bukkit.zPermissions.dao.MissingGroupException;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.storage.StorageStrategy;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

// TODO Break class up into player and group versions for better control over permissions
public class MetadataCommands {

    private final ZPermissionsCore core;

    private final StorageStrategy storageStrategy;

    private final CommandUuidResolver uuidResolver;

    private final boolean group;

    MetadataCommands(ZPermissionsCore core, StorageStrategy storageStrategy, CommandUuidResolver uuidResolver, boolean group) {
        this.core = core;
        this.storageStrategy = storageStrategy;
        this.group = group;
        this.uuidResolver = uuidResolver;
    }

    @Command(value="get", description="Retrieve metadata value")
    @Require({"zpermissions.player.view", "zpermissions.player.manage", "zpermissions.player.chat",
        "zpermissions.group.view", "zpermissions.group.manage", "zpermissions.group.chat"})
    public void get(CommandSender sender, final @Session("entityName") String name, final @Option("name") String metadataName) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                get(sender, name, uuid, metadataName);
            }
        });
    }

    private void get(CommandSender sender, final String name, final UUID uuid, final String metadataName) {
        Object result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction() throws Exception {
                return storageStrategy.getDao().getMetadata(name, uuid, group, metadataName);
            }
        }, true);
        
        if (result == null) {
            sendMessage(sender, colorize("%s%s{YELLOW} does not set {GOLD}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, metadataName);
            abortBatchProcessing();
        }
        else {
            sendMessage(sender, colorize("%s%s{YELLOW} sets {GOLD}%s{YELLOW} to {GREEN}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, metadataName, result);
        }
    }

    @Command(value="set", description="Set metadata (string)")
    @Require({"zpermissions.player.manage", "zpermissions.group.manage"})
    public void set(CommandSender sender, final @Session("entityName") String name, final @Option("name") String metadataName, @Option("value") String value, String[] rest) {
        final StringBuilder stringValue = new StringBuilder(value);
        if (rest.length > 0) {
            stringValue.append(' ')
                .append(ToHStringUtils.delimitedString(" ", (Object[])rest));
        }
        set0(sender, name, metadataName, stringValue.toString());
    }

    private void set0(CommandSender sender, final String name, final String metadataName, final Object value) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                set0(sender, name, uuid, metadataName, value);
            }
        });
    }

    private void set0(CommandSender sender, final String name, final UUID uuid, final String metadataName, final Object value) {
        try {
            storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallbackWithoutResult() {
                @Override
                public void doInTransactionWithoutResult() throws Exception {
                    storageStrategy.getDao().setMetadata(name, uuid, group, metadataName, value);
                }
            });
            core.invalidateMetadataCache(name, uuid, group);
        }
        catch (MissingGroupException e) {
            handleMissingGroup(sender, e);
            return;
        }

        sendMessage(sender, colorize("{GOLD}%s{YELLOW} set to {GREEN}%s{YELLOW} for %s%s"), metadataName, value == null ? Boolean.TRUE : value, group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name);
    }

    @Command(value="setint", description="Set metadata (integer)")
    @Require({"zpermissions.player.manage", "zpermissions.group.manage"})
    public void set(CommandSender sender, final @Session("entityName") String name, final @Option("name") String metadataName, @Option("value") long value) {
        set0(sender, name, metadataName, value);
    }

    @Command(value="setreal", description="Set metadata (real)")
    @Require({"zpermissions.player.manage", "zpermissions.group.manage"})
    public void set(CommandSender sender, final @Session("entityName") String name, final @Option("name") String metadataName, @Option("value") double value) {
        set0(sender, name, metadataName, value);
    }

    @Command(value="setbool", description="Set metadata (boolean)")
    @Require({"zpermissions.player.manage", "zpermissions.group.manage"})
    public void set(CommandSender sender, final @Session("entityName") String name, final @Option("name") String metadataName, @Option(value="value", optional=true) Boolean value) {
        set0(sender, name, metadataName, value == null ? Boolean.TRUE : value);
    }

    @Command(value="unset", description="Remove metadata value")
    @Require({"zpermissions.player.manage", "zpermissions.group.manage"})
    public void unset(CommandSender sender, final @Session("entityName") String name, final @Option("name") String metadataName) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                unset(sender, name, uuid, metadataName);
            }
        });
    }

    private void unset(CommandSender sender, final String name, final UUID uuid, final String metadataName) {
        Boolean result = storageStrategy.getRetryingTransactionStrategy().execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction() throws Exception {
                return storageStrategy.getDao().unsetMetadata(name, uuid, group, metadataName);
            }
        });
        
        if (result) {
            sendMessage(sender, colorize("{GOLD}%s{YELLOW} unset for %s%s"), metadataName, group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name);
            core.invalidateMetadataCache(name, uuid, group);
        }
        else {
            sendMessage(sender, colorize("%s%s{RED} does not set {GOLD}%s"), group ? ChatColor.DARK_GREEN : ChatColor.AQUA, name, metadataName);
            abortBatchProcessing();
        }
    }

    @Command(value={"show", "list", "ls"}, description="List all metadata")
    @Require({"zpermissions.player.view", "zpermissions.player.manage", "zpermissions.player.chat",
        "zpermissions.group.view", "zpermissions.group.manage", "zpermissions.group.chat"})
    public void list(CommandSender sender, final @Session("entityName") String name) {
        uuidResolver.resolveUsername(sender, name, group, new CommandUuidResolverHandler() {
            @Override
            public void process(CommandSender sender, String name, UUID uuid, boolean group) {
                list(sender, name, uuid);
            }
        });
    }

    private void list(CommandSender sender, final String name, final UUID uuid) {
        PermissionEntity entity = storageStrategy.getDao().getEntity(name, uuid, group);
        if (entity == null || entity.getMetadata().isEmpty()) {
            sendMessage(sender, colorize("{RED}%s has no metadata."), group ? "Group" : "Player");
            return;
        }

        for (EntityMetadata me : Utils.sortMetadata(entity.getMetadata())) {
            sendMessage(sender, colorize("{GOLD}%s{YELLOW}: {GREEN}%s"), me.getName(), me.getValue());
        }
    }

    private void handleMissingGroup(CommandSender sender, MissingGroupException e) {
        sendMessage(sender, colorize("{RED}Group {DARK_GREEN}%s{RED} does not exist."), e.getGroupName());
        abortBatchProcessing();
    }

}
