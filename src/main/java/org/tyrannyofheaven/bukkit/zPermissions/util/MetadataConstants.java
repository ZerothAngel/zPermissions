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
package org.tyrannyofheaven.bukkit.zPermissions.util;

/**
 * Simple holder for the names of common metadata properties. These properties
 * have no intrinsic significance but do affect Vault.
 * 
 * @author zerothangel
 */
public class MetadataConstants {

    public static final String PREFIX_KEY = "prefix";
    
    public static final String SUFFIX_KEY = "suffix";
    
    public static final String PRIMARY_GROUP_TRACK_KEY = "Vault.primary-group.track";

    private MetadataConstants() {
        throw new AssertionError("Don't instantiate me!");
    }

}
