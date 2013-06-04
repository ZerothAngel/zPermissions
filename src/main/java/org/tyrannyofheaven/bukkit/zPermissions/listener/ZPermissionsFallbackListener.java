/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
package org.tyrannyofheaven.bukkit.zPermissions.listener;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Listener installed if things don't initialize properly. Allows no one to
 * log in.
 * 
 * @author zerothangel
 */
public class ZPermissionsFallbackListener implements Listener {

    private static final String KICK_MESSAGE = "zPermissions failed to initialize";

    private final boolean kickOpsOnError;

    public ZPermissionsFallbackListener(boolean kickOpsOnError) {
        this.kickOpsOnError = kickOpsOnError;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (kickOpsOnError || !event.getPlayer().isOp())
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, KICK_MESSAGE);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().isOp()) {
            sendMessage(event.getPlayer(), colorize("{RED}zPermissions failed to initialize; All non-OP log-ins disallowed!"));
        }
    }

}
