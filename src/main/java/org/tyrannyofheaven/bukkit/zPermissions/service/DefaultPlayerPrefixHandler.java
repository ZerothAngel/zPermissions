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
package org.tyrannyofheaven.bukkit.zPermissions.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsConfig;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.util.MetadataConstants;

public class DefaultPlayerPrefixHandler implements PlayerPrefixHandler {

    private final ZPermissionsConfig config;

    public DefaultPlayerPrefixHandler(ZPermissionsConfig config) {
        this.config = config;
    }

    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.vault.PlayerPrefixHandler#getPlayerPrefix(java.lang.String)
     */
    @Override
    public String getPlayerPrefix(ZPermissionsService service, UUID uuid) {
        String prefix;
        
        if (config.getVaultPlayerPrefixFormat().isEmpty()) {
            prefix = service.getPlayerMetadata(uuid, MetadataConstants.PREFIX_KEY, String.class);
            if (prefix == null && config.isVaultPrefixIncludesGroup())
                prefix = service.getGroupMetadata(service.getPlayerPrimaryGroup(uuid), MetadataConstants.PREFIX_KEY, String.class);
        }
        else {
            prefix = getFormattedPrefixSuffix(service, uuid, config.getVaultPlayerPrefixFormat(), true);
        }
        
        if (prefix == null)
            return "";
        else
            return prefix;
    }
    
    /* (non-Javadoc)
     * @see org.tyrannyofheaven.bukkit.zPermissions.vault.PlayerPrefixHandler#getPlayerSuffix(java.lang.String)
     */
    @Override
    public String getPlayerSuffix(ZPermissionsService service, UUID uuid) {
        String suffix;
        
        if (config.getVaultPlayerSuffixFormat().isEmpty()) {
            suffix = service.getPlayerMetadata(uuid, MetadataConstants.SUFFIX_KEY, String.class);
            if (suffix == null && config.isVaultPrefixIncludesGroup())
                suffix = service.getGroupMetadata(service.getPlayerPrimaryGroup(uuid), MetadataConstants.SUFFIX_KEY, String.class);
        }
        else {
            suffix = getFormattedPrefixSuffix(service, uuid, config.getVaultPlayerSuffixFormat(), false);
        }
        
        if (suffix == null)
            return "";
        else
            return suffix;
    }

    private String getFormattedPrefixSuffix(ZPermissionsService service, UUID uuid, String format, boolean isPrefix) {
        Map<String, String> subMap = new HashMap<>();
        // Scan format, only calculate tokens that exist in it
        if (format.contains("%p")) {
            // Player
            String value = service.getPlayerMetadata(uuid, isPrefix ? MetadataConstants.PREFIX_KEY : MetadataConstants.SUFFIX_KEY, String.class);
            if (value == null) value = "";
            subMap.put("%p", value);
        }
        if (format.contains("%g")) {
            // Primary Group
            String value = service.getGroupMetadata(service.getPlayerPrimaryGroup(uuid), isPrefix ? MetadataConstants.PREFIX_KEY : MetadataConstants.SUFFIX_KEY, String.class);
            if (value == null) value = "";
            subMap.put("%g", value);
        }
        if (format.contains("%a")) {
            // All Groups
            List<String> groups = getPlayerGroups(service, uuid);
            Collections.reverse(groups); // groups is in application order. We actually want it in display order.
            StringBuilder sb = new StringBuilder();
            for (String group : groups) {
                String value = service.getGroupMetadata(group, isPrefix ? MetadataConstants.PREFIX_KEY : MetadataConstants.SUFFIX_KEY, String.class);
                if (value == null) value = "";
                sb.append(value);
            }
            subMap.put("%a", sb.toString());
        }
        if (format.contains("%A")) {
            // All Groups Reversed
            List<String> groups = getPlayerGroups(service, uuid);
            StringBuilder sb = new StringBuilder();
            for (String group : groups) {
                String value = service.getGroupMetadata(group, isPrefix ? MetadataConstants.PREFIX_KEY : MetadataConstants.SUFFIX_KEY, String.class);
                if (value == null) value = "";
                sb.append(value);
            }
            subMap.put("%A", sb.toString());
        }

        // Perform substitution
        String result = format;
        for (Map.Entry<String, String> me : subMap.entrySet()) {
            result = result.replaceAll(me.getKey(), me.getValue());
        }
        return result;
    }

    private List<String> getPlayerGroups(ZPermissionsService service, UUID uuid) {
        if (config.isVaultGetGroupsUsesAssignedOnly())
            return service.getPlayerAssignedGroups(uuid);
        else
            return new ArrayList<>(service.getPlayerGroups(uuid));
    }

}
