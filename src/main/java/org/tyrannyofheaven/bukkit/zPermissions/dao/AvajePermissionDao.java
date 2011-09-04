package org.tyrannyofheaven.bukkit.zPermissions.dao;

import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Owner;

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
            Owner owner = getEbeanServer().find(Owner.class).where()
                .eq("name", name)
                .eq("group", false)
                .findUnique();
            if (owner != null) {
                // FIXME
                Entry found = null;
                for (Entry entry : owner.getPermissions()) {
                    if (entry.getPermission().equals(permission)) {
                        found = entry;
                        break;
                    }
                }
                
                if (found != null)
                    return found.isValue();
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
            Owner owner = getEbeanServer().find(Owner.class).where()
                .eq("name", name)
                .eq("group", false)
                .findUnique();
            if (owner == null) {
                owner = new Owner();
                owner.setName(name);
                owner.setGroup(false);
            }

            Entry found = null;
            for (Entry entry : owner.getPermissions()) {
                if (entry.getPermission().equals(permission)) {
                    found = entry;
                    break;
                }
            }
            
            if (found == null) {
                found = new Entry();
                owner.getPermissions().add(found);
                found.setOwner(owner);
                found.setPermission(permission);
            }
            
            found.setValue(value);

            getEbeanServer().save(owner);
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
