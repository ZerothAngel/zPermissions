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

import java.util.List;

import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public interface PermissionDao {

    public Boolean getPermission(String name, boolean group, String world, String permission);

    public void setPermission(String name, boolean group, String world, String permission, boolean value);

    public void unsetPermission(String name, boolean group, String world, String permission);

    public void addMember(String groupName, String member);
    
    public void removeMember(String groupName, String member);

    public List<PermissionEntity> getGroups(String member);

    public PermissionEntity getEntity(String name, boolean group);

    public List<PermissionEntity> getEntities(boolean group);

    public void setGroup(String playerName, String groupName);

    public void setParent(String groupName, String parentName);

    public void setPriority(String groupName, int priority);

}
