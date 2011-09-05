package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.List;
import java.util.Set;

import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public interface PermissionDao {

    public Boolean getPermission(String name, boolean group, String world, String permission);

    public void setPermission(String name, boolean group, String world, String permission, boolean value);

    public void unsetPermission(String name, boolean group, String world, String permission);

    public void addMember(String groupName, String member);
    
    public void removeMember(String groupName, String member);

    public Set<PermissionEntity> getGroups(String member);

    public PermissionEntity getEntity(String name, boolean group);

    public List<PermissionEntity> getEntities(boolean group);

    public void setGroup(String playerName, String groupName);

    public void setParent(String groupName, String parentName);

}
