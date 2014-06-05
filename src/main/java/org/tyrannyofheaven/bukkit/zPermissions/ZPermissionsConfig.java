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

import java.io.File;
import java.util.List;

/**
 * Holds configuration data used by other modules, namely the command handlers.
 * 
 * @author asaddi
 */
public interface ZPermissionsConfig {

    public List<String> getTracks();

    public String getDefaultTrack();

    public List<String> getTrack(String trackName);

    public File getDumpDirectory();

    public boolean isRankAdminBroadcast();

    public int getDefaultTempPermissionTimeout();

    public String getDefaultPrimaryGroupTrack();

    public boolean isVaultPrefixIncludesGroup();

    public boolean isVaultMetadataIncludesGroup();

    public boolean isVaultGroupTestUsesAssignedOnly();
    
    public boolean isVaultGetGroupsUsesAssignedOnly();

    public boolean isInheritedMetadata();

    public String getVaultPlayerPrefixFormat();
    
    public String getVaultPlayerSuffixFormat();

    public int getSearchBatchSize();
    
    public int getSearchDelay();

    public boolean isServiceMetadataPrefixHack();

}
