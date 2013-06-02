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
package org.tyrannyofheaven.bukkit.zPermissions;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Bukkit event fired off whenever zPermissions might potentially change an
 * online player's permissions. This includes world changes, commands that affect
 * the groups or permissions of a player, as well as certain movement-based
 * events (when region-support is enabled).
 * 
 * <p>Note that unconditional permissions changes that occur during
 * standard Bukkit events (namely: PlayerLoginEvent, PlayerJoinEvent,
 * PlayerQuitEvent) do not fire off this event.
 * 
 * <p>Also note that this event may be called even if the player's permissions
 * did not actually change. However, for its PlayerMoveEvent handler (when
 * region-support is enabled), zPermissions guarantees that this event will be
 * called only when the player crosses a region boundary.
 */
public class ZPermissionsPlayerUpdateEvent extends PlayerEvent {

    private final Cause cause;

    /**
     * Creates a player permissions change event.
     * 
     * @param who the affected player
     * @param cause the cause of this permissions change
     */
    public ZPermissionsPlayerUpdateEvent(Player who, Cause cause) {
        super(who);
        if (cause == null)
            throw new IllegalArgumentException("cause cannot be null");
        this.cause = cause;
    }

    /**
     * Returns the cause of this permissions change.
     * 
     * @return the cause of this permissions change
     */
    public Cause getCause() {
        return cause;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public static enum Cause {
        /**
         * Permissions change to the player caused by a command. Includes changes
         * through the API. Note that the settemp command cannot be covered since
         * there's no visibility as to when the permission goes away.
         */
        COMMAND,
        
        /**
         * An associated group was changed. Includes command line, changes
         * through the API, group membership expirations, and server-wide changes
         * (reload, refresh, etc. since these force a re-read from storage).
         */
        GROUP_CHANGE,

        /**
         * Player movement, including movement within the same world (if
         * region-support is enabled) and movement to a different world.
         */
        MOVEMENT;
    }

}
