package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.Set;

import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public interface PermissionDao {

    public Boolean getPermission(String name, boolean group, String permission);

    public void setPermission(String name, boolean group, String permission, boolean value);

    public void unsetPermission(String name, boolean group, String permission);

    public void addMember(String groupName, String member);
    
    public void removeMember(String groupName, String member);

    public Set<PermissionEntity> getGroups(String member);

}
