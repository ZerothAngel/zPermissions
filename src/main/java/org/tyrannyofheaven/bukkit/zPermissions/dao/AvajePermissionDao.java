package org.tyrannyofheaven.bukkit.zPermissions.dao;

import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Player;

import com.avaje.ebean.EbeanServer;

public class AvajePermissionDao implements PermissionDao {

    private final EbeanServer ebean;
    
    public AvajePermissionDao(EbeanServer ebean) {
        this.ebean = ebean;
    }

    private EbeanServer getEbeanServer() {
        return ebean;
    }

    @Override
    public Boolean getPlayerPermission(String name, String permission) {
        getEbeanServer().beginTransaction();
        try {
            Player player = getEbeanServer().find(Player.class).where()
                .eq("name", name)
                .findUnique();
            if (player != null) {
                Entry entry = getEbeanServer().find(Entry.class).where()
                    .eq("owner", player)
                    .eq("name", permission)
                    .findUnique();
                if (entry != null)
                    return entry.isValue();
            }
            return null;
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Override
    public void setPlayerPermission(String name, String permission, boolean value) {
        getEbeanServer().beginTransaction();
        try {
            Player player = getEbeanServer().find(Player.class).where()
                .eq("name", name)
                .findUnique();
            if (player == null) {
                player = new Player();
                player.setName(name);
            }

            Entry found = null;
            for (Entry entry : player.getPermissions()) {
                if (entry.getPermission().equals(permission)) {
                    found = entry;
                    break;
                }
            }
            
            if (found == null) {
                found = new Entry();
                player.getPermissions().add(found);
                found.setOwner(player);
                found.setPermission(permission);
            }
            
            found.setValue(value);

            getEbeanServer().save(player);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Override
    public void unsetPlayerPermission(String name, String permission) {
        // TODO Auto-generated method stub

    }

}
