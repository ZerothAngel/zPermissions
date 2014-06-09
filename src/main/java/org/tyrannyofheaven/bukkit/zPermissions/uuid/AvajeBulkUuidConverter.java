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

import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.log;
import static org.tyrannyofheaven.bukkit.util.ToHLoggingUtils.warn;
import static org.tyrannyofheaven.bukkit.util.uuid.UuidUtils.SHORT_UUID_RE;
import static org.tyrannyofheaven.bukkit.util.uuid.UuidUtils.canonicalizeUuid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.uuid.UuidDisplayName;
import org.tyrannyofheaven.bukkit.util.uuid.UuidResolver;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

import com.avaje.ebean.EbeanServer;

public class AvajeBulkUuidConverter implements BulkUuidConverter {

    private final Plugin plugin;

    private final EbeanServer ebeanServer;

    private final UuidResolver uuidResolver;

    public AvajeBulkUuidConverter(Plugin plugin, UuidResolver uuidResolver) {
        this.plugin = plugin;
        this.ebeanServer = plugin.getDatabase();
        this.uuidResolver = uuidResolver;
    }

    private EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    private UuidResolver getUuidResolver() {
        return uuidResolver;
    }

    @Override
    public void migrate() throws Exception {
        getEbeanServer().beginTransaction();
        try {
            // Gather everything
            Set<String> usernames = new HashSet<>();
            List<PermissionEntity> entities = prepareEntities(usernames);
            log(plugin, "%d entit%s to migrate", entities.size(), entities.size() == 1 ? "y" : "ies");
            List<Membership> memberships = migrateMemberships(usernames);
            log(plugin, "%d membership%s to migrate", memberships.size(), memberships.size() == 1 ? "" : "s");

            if (entities.size() == 0 && memberships.size() == 0) {
                log(plugin, "Nothing to migrate");
                return;
            }

            // Do bulk lookup
            log(plugin, "Looking up %d UUID%s...", usernames.size(), usernames.size() == 1 ? "" : "s");
            Map<String, UuidDisplayName> resolved = getUuidResolver().resolve(usernames);

            // Migrate
            log(plugin, "Migrating entities...");
            List<PermissionEntity> entitiesToDelete = migrateEntities(entities, resolved);
            log(plugin, "Migrating memberships...");
            List<Membership> membershipsToDelete = migrateMemberships(memberships, resolved);

            // Delete failures
            log(plugin, "Deleting entities that failed to migrate...");
            getEbeanServer().delete(entitiesToDelete);
            // NB Memberships theoretically wholly independent of (player) entities
            // So no need to refresh or delete memberships first.
            log(plugin, "Deleting memberships that failed to migrate...");
            getEbeanServer().delete(membershipsToDelete);

            // Commit
            getEbeanServer().commitTransaction();
            
            log(plugin, "Migration done");
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    private List<PermissionEntity> prepareEntities(Set<String> usernames) {
        List<PermissionEntity> toConvert = new ArrayList<>();
        for (PermissionEntity entity : getEbeanServer().createQuery(PermissionEntity.class).where().eq("group", false).findList()) {
            // Does it need converting?
            // The big clue is when name == displayName
            if (entity.getName().equalsIgnoreCase(entity.getDisplayName())) {
                // But for sanity's sake, make sure name doesn't look like a short UUID
                // (This could be the case when a UUID/DisplayName is parsed without the DisplayName)
                Matcher m = SHORT_UUID_RE.matcher(entity.getName());
                if (!m.matches()) {
                    toConvert.add(entity);
                    usernames.add(entity.getDisplayName().toLowerCase());
                }
            }
        }
        return toConvert;
    }

    private List<Membership> migrateMemberships(Set<String> usernames) {
        List<Membership> toConvert = new ArrayList<>();
        for (Membership membership : getEbeanServer().createQuery(Membership.class).findList()) {
            // Does it need converting?
            // Same criteria as for entities
            if (membership.getMember().equalsIgnoreCase(membership.getDisplayName())) {
                Matcher m = SHORT_UUID_RE.matcher(membership.getMember());
                if (!m.matches()) {
                    toConvert.add(membership);
                    usernames.add(membership.getDisplayName().toLowerCase());
                }
            }
        }
        return toConvert;
    }

    private List<PermissionEntity> migrateEntities(Collection<PermissionEntity> entities, Map<String, UuidDisplayName> resolved) {
        List<PermissionEntity> toSave = new ArrayList<>();
        List<PermissionEntity> toDelete = new ArrayList<>();
        for (PermissionEntity entity : entities) {
            UuidDisplayName udn = resolved.get(entity.getDisplayName().toLowerCase());
            if (udn != null) {
                entity.setName(canonicalizeUuid(udn.getUuid()));
                entity.setDisplayName(udn.getDisplayName());
                toSave.add(entity);
            }
            else {
                toDelete.add(entity);
                warn(plugin, "Unable to migrate '%s' -- failed to lookup UUID", entity.getDisplayName());
            }
        }
        getEbeanServer().save(toSave);
        return toDelete;
    }

    private List<Membership> migrateMemberships(Collection<Membership> memberships, Map<String, UuidDisplayName> resolved) {
        List<Membership> toSave = new ArrayList<>();
        List<Membership> toDelete = new ArrayList<>();
        for (Membership membership : memberships) {
            UuidDisplayName udn = resolved.get(membership.getDisplayName().toLowerCase());
            if (udn != null) {
                membership.setMember(canonicalizeUuid(udn.getUuid()));
                membership.setDisplayName(udn.getDisplayName());
                toSave.add(membership);
            }
            else {
                toDelete.add(membership);
                warn(plugin, "Unable to migrate '%s' (member of '%s') -- failed to lookup UUID", membership.getDisplayName(), membership.getGroup().getDisplayName());
            }
        }
        getEbeanServer().save(toSave);
        return toDelete;
    }

}
