/*
 * Copyright 2014 ZerothAngel <zerothangel@tyrannyofheaven.org>
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

import org.bukkit.command.CommandSender;
import org.tyrannyofheaven.bukkit.util.command.Command;
import org.tyrannyofheaven.bukkit.util.command.Option;
import org.tyrannyofheaven.bukkit.util.uuid.UuidResolver;

public class UuidCacheCommands {

    private final UuidResolver uuidResolver;

    public UuidCacheCommands(UuidResolver uuidResolver) {
        this.uuidResolver = uuidResolver;
    }

    @Command(value="invalidate", description="Invalidate cache entry for a single name")
    public void invalidate(CommandSender sender, @Option(value="player", completer="player") String playerName) {
        uuidResolver.invalidate(playerName);
    }

    @Command(value="invalidate-all", description="Invalidate all cache entries")
    public void invalidate(CommandSender sender) {
        uuidResolver.invalidateAll();
    }

}
