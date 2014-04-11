/*
 * Copyright 2011 Allan Saddi <allan@saddi.com>
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
package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

/**
 * Data access object for zPermissions. This isn't actually a pure DAO as it
 * contains some business logic &mdash; {@link #setGroup(UUID, String, String, Date)} being the
 * biggest offender. Ah well... :)
 * 
 * <p>More like a DAO/service object, I guess!
 * 
 * <p>Methods should be self-explanatory. I'm not gonna write javadocs for each and
 * every one! (yet) :P
 * 
 * @author asaddi
 */
public interface PermissionDao {

    public Boolean getPermission(String name, UUID uuid, boolean group, String region, String world, String permission);

    public void setPermission(String name, UUID uuid, boolean group, String region, String world, String permission, boolean value);

    public boolean unsetPermission(String name, UUID uuid, boolean group, String region, String world, String permission);

    public void addMember(String groupName, UUID memberUuid, String memberName, Date expiration);
    
    public boolean removeMember(String groupName, UUID memberUuid);

    // NB: Resolver critical path
    public List<Membership> getGroups(UUID memberUuid);

    public List<Membership> getMembers(String group);

    public PermissionEntity getEntity(String name, UUID uuid, boolean group);

    public List<PermissionEntity> getEntities(boolean group);

    public void setGroup(UUID playerUuid, String playerName, String groupName, Date expiration);

    // Technically deprecated
    public void setParent(String groupName, String parentName);

    public void setParents(String groupName, List<String> parentNames);

    public void setPriority(String groupName, int priority);

    public boolean deleteEntity(String name, UUID uuid, boolean group);

    // NB: Resolver critical path
    public List<String> getAncestry(String groupName);

    // NB: Resolver critical path
    public List<Entry> getEntries(String name, UUID uuid, boolean group);

    public boolean createGroup(String name);
    
    public List<String> getEntityNames(boolean group);

    public Object getMetadata(String name, UUID uuid, boolean group, String metadataName);

    public List<EntityMetadata> getAllMetadata(String name, UUID uuid, boolean group);

    public void setMetadata(String name, UUID uuid, boolean group, String metadataName, Object value);

    public boolean unsetMetadata(String name, UUID uuid, boolean group, String metadataName);

    public void updateDisplayName(UUID uuid, String displayName);

}
