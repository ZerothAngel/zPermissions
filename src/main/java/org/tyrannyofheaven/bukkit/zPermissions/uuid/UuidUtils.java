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
package org.tyrannyofheaven.bukkit.zPermissions.uuid;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UuidUtils {

    public static final Pattern SHORT_UUID_RE = Pattern.compile("^\\p{XDigit}{32}$");

    private static final Pattern UUID_NAME_RE = Pattern.compile("^(\\p{XDigit}{32}|\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12})(?:/(.+))?$");

    private UuidUtils() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static String longUuidToShort(String uuidString) {
        if (uuidString.length() != 36)
            throw new IllegalArgumentException("Wrong length");
        return uuidString.replaceAll("-", "");
    }

    public static String shortUuidToLong(String uuidString) {
        if (uuidString.length() != 32)
            throw new IllegalArgumentException("Wrong length");
        return uuidString.substring(0, 8) + "-" + uuidString.substring(8, 12) + "-" + uuidString.substring(12, 16) + "-" + uuidString.substring(16, 20) + "-" + uuidString.substring(20, 32);
    }

    public static String canonicalizeUuid(UUID memberUuid) {
        return longUuidToShort(memberUuid.toString()).toLowerCase();
    }

    public static UUID uncanonicalizeUuid(String shortUuid) {
        return UUID.fromString(shortUuidToLong(shortUuid));
    }

    public static UuidDisplayName parseUuidDisplayName(String name) {
        Matcher m = UUID_NAME_RE.matcher(name);
        if (m.matches()) {
            String uuidString = m.group(1);
            String displayName = m.group(2);
            
            if (uuidString.length() == 32)
                uuidString = shortUuidToLong(uuidString);
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidString);
            }
            catch (IllegalArgumentException e) {
                return null;
            }
            return new UuidDisplayName(uuid, displayName);
        }
        return null;
    }

    public static String formatPlayerName(UUID uuid, String displayName, boolean showUuid) {
        if (showUuid)
            return canonicalizeUuid(uuid) + "/" + displayName;
        else
            return displayName;
    }

}
