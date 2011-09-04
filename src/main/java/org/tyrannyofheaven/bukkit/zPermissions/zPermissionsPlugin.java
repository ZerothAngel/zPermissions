package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.util.ToHUtils;
import org.tyrannyofheaven.bukkit.util.command.ToHCommandExecutor;
import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Group;
import org.tyrannyofheaven.bukkit.zPermissions.model.Owner;
import org.tyrannyofheaven.bukkit.zPermissions.model.Player;

public class zPermissionsPlugin extends JavaPlugin {

    private PermissionDao dao;

    PermissionDao getDao() {
        return dao;
    }

    @Override
    public void onDisable() {
        log("%s disabled.", getDescription().getVersion());
    }

    @Override
    public void onEnable() {
        log("%s enabled.", getDescription().getVersion());
        
        try {
            getDatabase().createQuery(Entry.class).findRowCount();
        }
        catch (PersistenceException e) {
            log("Creating SQL tables...");
            installDDL();
            log("Done.");
        }
        
        dao = new AvajePermissionDao(getDatabase());
        
        getCommand("perm").setExecutor(new ToHCommandExecutor<zPermissionsPlugin>(this, new RootCommand()));
    }

    public void log(String format, Object... args) {
        ToHUtils.log(this, Level.INFO, format, args);
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        List<Class<?>> result = new ArrayList<Class<?>>();
        result.add(Owner.class);
        result.add(Player.class);
        result.add(Group.class);
        result.add(Entry.class);
        return result;
    }

}
