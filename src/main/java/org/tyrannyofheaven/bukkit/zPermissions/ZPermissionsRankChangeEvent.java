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

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Bukkit event fired off whenever a rank command successfully completes.
 */
public class ZPermissionsRankChangeEvent extends Event {

    private final String playerName;

    private final String track;

    private final String fromGroup;

    private final String toGroup;

    /**
     * Create a rank change event.
     * 
     * @param playerName the name of the player
     * @param track the name of the track
     * @param fromGroup the previous group. May be <code>null</code>.
     * @param toGroup the new group. May be <code>null</code>.
     */
    public ZPermissionsRankChangeEvent(String playerName, String track, String fromGroup, String toGroup) {
        if (playerName == null || playerName.trim().isEmpty())
            throw new IllegalArgumentException("playerName must have a value");
        if (track == null || track.trim().isEmpty())
            throw new IllegalArgumentException("track must have a value");
        if (fromGroup != null && fromGroup.trim().isEmpty())
            fromGroup = null;
        if (toGroup != null && toGroup.trim().isEmpty())
            toGroup = null;

        this.playerName = playerName;
        this.track = track;
        this.fromGroup = fromGroup;
        this.toGroup = toGroup;
    }

    /**
     * The name of the player whose rank has changed. Note that there's no
     * guarantee about the letter case of the name (it will be as was entered
     * from the command line).
     * 
     * @return the name of the player
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * The name of the track. Note that there's no guarantee about the letter
     * case of the name.
     * 
     * @return the name of the track
     */
    public String getTrack() {
        return track;
    }

    /**
     * The player's previous group. This may be <code>null</code> meaning the
     * player was not previously on any group in this track.
     * 
     * @return the previous group
     */
    public String getFromGroup() {
        return fromGroup;
    }

    /**
     * The player's new group. This may be <code>null</code> meaning the player
     * was removed from all groups in this track.
     * 
     * @return the new group
     */
    public String getToGroup() {
        return toGroup;
    }

    private static final HandlerList handlers = new HandlerList();

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
