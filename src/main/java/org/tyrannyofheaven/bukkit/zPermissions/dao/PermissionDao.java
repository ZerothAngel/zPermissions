package org.tyrannyofheaven.bukkit.zPermissions.dao;

public interface PermissionDao {

    public Boolean getPlayerPermission(String name, String permission);

    public void setPlayerPermission(String name, String permission, boolean value);

    public void unsetPlayerPermission(String name, String permission);

}
