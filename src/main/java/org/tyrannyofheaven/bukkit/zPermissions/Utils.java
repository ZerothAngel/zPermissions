/*
 * Copyright 2012 Allan Saddi <allan@saddi.com>
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

/**
 * Collection of static utils, constants, etc.
 * 
 * @author asaddi
 */
public class Utils {

    public final static Comparator<PermissionEntity> PERMISSION_ENTITY_ALPHA_COMPARATOR = new Comparator<PermissionEntity>() {
        @Override
        public int compare(PermissionEntity a, PermissionEntity b) {
            return a.getDisplayName().compareTo(b.getDisplayName());
        }
    };

    public final static Comparator<Entry> ENTRY_COMPARATOR = new Comparator<Entry>() {
        @Override
        public int compare(Entry a, Entry b) {
            if (a.getRegion() != null && b.getRegion() == null)
                return 1;
            else if (a.getRegion() == null && b.getRegion() != null)
                return -1;
            else if (a.getRegion() != null && b.getRegion() != null) {
                int regions = a.getRegion().getName().compareTo(b.getRegion().getName());
                if (regions != 0) return regions;
            }

            if (a.getWorld() != null && b.getWorld() == null)
                return 1;
            else if (a.getWorld() == null && b.getWorld() != null)
                return -1;
            else if (a.getWorld() != null && b.getWorld() != null) {
                int worlds = a.getWorld().getName().compareTo(b.getWorld().getName());
                if (worlds != 0) return worlds;
            }

            return a.getPermission().compareTo(b.getPermission());
        }
    };

    public static List<Entry> sortPermissions(Collection<Entry> entries) {
        List<Entry> result = new ArrayList<Entry>(entries);
        Collections.sort(result, ENTRY_COMPARATOR);
        return result;
    }

}
