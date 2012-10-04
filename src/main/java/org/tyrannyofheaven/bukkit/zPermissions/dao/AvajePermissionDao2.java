package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

import com.avaje.ebean.EbeanServer;

public class AvajePermissionDao2 extends BaseMemoryPermissionDao {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final EbeanServer ebeanServer;

    public AvajePermissionDao2(EbeanServer ebeanServer) {
        this.ebeanServer = ebeanServer;
    }

    private EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    @Override
    synchronized PermissionRegion getRegion(String region) {
        return super.getRegion(region);
    }

    @Override
    synchronized PermissionWorld getWorld(String world) {
        return super.getWorld(world);
    }

    @Override
    synchronized public Boolean getPermission(String name, boolean group, String region, String world, String permission) {
        return super.getPermission(name, group, region, world, permission);
    }

    @Override
    synchronized public void setPermission(String name, boolean group, String region, String world, String permission, boolean value) {
        super.setPermission(name, group, region, world, permission, value);
    }

    @Override
    synchronized public boolean unsetPermission(String name, boolean group, String region, String world, String permission) {
        return super.unsetPermission(name, group, region, world, permission);
    }

    @Override
    synchronized public void addMember(String groupName, String member) {
        super.addMember(groupName, member);
    }

    @Override
    synchronized public boolean removeMember(String groupName, String member) {
        return super.removeMember(groupName, member);
    }

    @Override
    synchronized public List<String> getGroups(String member) {
        return super.getGroups(member);
    }

    @Override
    synchronized public List<String> getMembers(String group) {
        return super.getMembers(group);
    }

    @Override
    synchronized public PermissionEntity getEntity(String name, boolean group) {
        return super.getEntity(name, group);
    }

    @Override
    synchronized public List<PermissionEntity> getEntities(boolean group) {
        return super.getEntities(group);
    }

    @Override
    synchronized public void setGroup(String playerName, String groupName) {
        super.setGroup(playerName, groupName);
    }

    @Override
    synchronized public void setParent(String groupName, String parentName) {
        super.setParent(groupName, parentName);
    }

    @Override
    synchronized public void setPriority(String groupName, int priority) {
        super.setPriority(groupName, priority);
    }

    @Override
    synchronized public boolean deleteEntity(String name, boolean group) {
        return super.deleteEntity(name, group);
    }

    @Override
    synchronized public List<String> getAncestry(String groupName) {
        return super.getAncestry(groupName);
    }

    @Override
    synchronized public List<Entry> getEntries(String name, boolean group) {
        return super.getEntries(name, group);
    }

    @Override
    synchronized public boolean createGroup(String name) {
        return super.createGroup(name);
    }

    @Override
    synchronized public List<String> getEntityNames(boolean group) {
        return super.getEntityNames(group);
    }

    @Override
    protected void createRegion(PermissionRegion region) {
        PermissionRegion dbRegion = getEbeanServer().find(PermissionRegion.class).where()
                .eq("name", region.getName().toLowerCase())
                .findUnique();
        if (dbRegion == null) {
            dbRegion = new PermissionRegion();
            dbRegion.setName(region.getName().toLowerCase());
            getEbeanServer().save(dbRegion);
        }
    }

    @Override
    protected void createWorld(PermissionWorld world) {
        PermissionWorld dbWorld = getEbeanServer().find(PermissionWorld.class).where()
                .eq("name", world.getName().toLowerCase())
                .findUnique();
        if (dbWorld == null) {
            dbWorld = new PermissionWorld();
            dbWorld.setName(world.getName().toLowerCase());
            getEbeanServer().save(dbWorld);
        }
    }

    @Override
    protected void createEntity(PermissionEntity entity) {
        PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", entity.getName().toLowerCase())
                .eq("group", entity.isGroup())
                .findUnique();
        if (dbEntity == null) {
            dbEntity = new PermissionEntity();
            dbEntity.setName(entity.getName().toLowerCase());
            dbEntity.setGroup(entity.isGroup());
            dbEntity.setDisplayName(entity.getDisplayName());
            // NB assumes name/group/displayName are only attributes that need saving
            getEbeanServer().save(dbEntity);
        }
    }

    @Override
    protected void createOrUpdateEntry(Entry entry) {
        // Locate dependent objects
        PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", entry.getEntity().getName().toLowerCase())
                .eq("group", entry.getEntity().isGroup())
                .findUnique();
        if (entity == null) {
            entity = inconsistentEntity(entry.getEntity().getDisplayName(), entry.getEntity().isGroup());
        }

        PermissionRegion region = null;
        if (entry.getRegion() != null) {
            region = getEbeanServer().find(PermissionRegion.class).where()
                    .eq("name", entry.getRegion().getName().toLowerCase())
                    .findUnique();
            if (region == null) {
                region = inconsistentRegion(entry.getRegion().getName());
            }
        }
        
        PermissionWorld world = null;
        if (entry.getWorld() != null) {
            world = getEbeanServer().find(PermissionWorld.class).where()
                    .eq("name", entry.getWorld().getName().toLowerCase())
                    .findUnique();
            if (world == null) {
                world = inconsistentWorld(entry.getWorld().getName());
            }
        }
        
        Entry dbEntry = getEbeanServer().find(Entry.class).where()
                .eq("entity", entity)
                .eq("region", region)
                .eq("world", world)
                .eq("permission", entry.getPermission().toLowerCase())
                .findUnique();
        if (dbEntry == null) {
            dbEntry = new Entry();
            dbEntry.setEntity(entity);
            dbEntry.setRegion(region);
            dbEntry.setWorld(world);
            dbEntry.setPermission(entry.getPermission().toLowerCase());
        }
        
        dbEntry.setValue(entry.isValue());
        getEbeanServer().save(dbEntry);
    }

    @Override
    protected void deleteEntry(Entry entry) {
        // Locate dependent objects
        PermissionEntity entity = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", entry.getEntity().getName().toLowerCase())
                .eq("group", entry.getEntity().isGroup())
                .findUnique();
        if (entity == null) {
            databaseInconsistency();
            return;
        }

        PermissionRegion region = null;
        if (entry.getRegion() != null) {
            region = getEbeanServer().find(PermissionRegion.class).where()
                    .eq("name", entry.getRegion().getName().toLowerCase())
                    .findUnique();
            if (region == null) {
                databaseInconsistency();
                return;
            }
        }
        
        PermissionWorld world = null;
        if (entry.getWorld() != null) {
            world = getEbeanServer().find(PermissionWorld.class).where()
                    .eq("name", entry.getWorld().getName().toLowerCase())
                    .findUnique();
            if (world == null) {
                databaseInconsistency();
                return;
            }
        }
        
        Entry dbEntry = getEbeanServer().find(Entry.class).where()
                .eq("entity", entity)
                .eq("region", region)
                .eq("world", world)
                .eq("permission", entry.getPermission().toLowerCase())
                .findUnique();
        if (dbEntry == null) {
            databaseInconsistency();
            return;
        }

        getEbeanServer().delete(dbEntry);
    }

    @Override
    protected void createMembership(Membership membership) {
        // Locate dependent object
        PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", membership.getGroup().getName().toLowerCase())
                .eq("group", true)
                .findUnique();
        if (group == null) {
            group = inconsistentEntity(membership.getGroup().getDisplayName(), true);
        }

        Membership dbMembership = getEbeanServer().find(Membership.class).where()
                .eq("group", group)
                .eq("member", membership.getMember().toLowerCase())
                .findUnique();
        if (dbMembership == null) {
            dbMembership = new Membership();
            dbMembership.setGroup(group);
            dbMembership.setMember(membership.getMember().toLowerCase());
            getEbeanServer().save(dbMembership);
        }
    }

    @Override
    protected void deleteEntity(PermissionEntity entity) {
        PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", entity.getName().toLowerCase())
                .eq("group", entity.isGroup())
                .findUnique();
        if (dbEntity == null) {
            databaseInconsistency();
            return;
        }
        
        if (dbEntity.isGroup()) {
            // Break parent/child relationship
            for (PermissionEntity child : dbEntity.getChildren()) {
                child.setParent(null);
                getEbeanServer().save(child);
            }
        }

        getEbeanServer().delete(dbEntity);
    }

    @Override
    protected void deleteMembership(Membership membership) {
        // Locate dependent object
        PermissionEntity group = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", membership.getGroup().getName().toLowerCase())
                .eq("group", true)
                .findUnique();
        if (group == null) {
            databaseInconsistency();
            return;
        }

        Membership dbMembership = getEbeanServer().find(Membership.class).where()
                .eq("group", group)
                .eq("member", membership.getMember().toLowerCase())
                .findUnique();
        if (dbMembership == null) {
            databaseInconsistency();
            return;
        }
        
        getEbeanServer().delete(dbMembership);
    }

    @Override
    protected void setEntityParent(PermissionEntity entity, PermissionEntity parent) {
        PermissionEntity dbParent = null;
        if (parent != null) {
            dbParent = getEbeanServer().find(PermissionEntity.class).where()
                    .eq("name", parent.getName().toLowerCase())
                    .eq("group", true)
                    .findUnique();
            if (dbParent == null) {
                dbParent = inconsistentEntity(parent.getDisplayName(), true);
            }
        }
        
        PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", entity.getName().toLowerCase())
                .eq("group", true)
                .findUnique();
        if (dbEntity == null) {
            dbEntity = inconsistentEntity(entity.getDisplayName(), true);
        }
        
        dbEntity.setParent(dbParent);
        getEbeanServer().save(dbEntity);
    }

    @Override
    protected void setEntityPriority(PermissionEntity entity, int priority) {
        PermissionEntity dbEntity = getEbeanServer().find(PermissionEntity.class).where()
                .eq("name", entity.getName().toLowerCase())
                .eq("group", true)
                .findUnique();
        if (dbEntity == null) {
            dbEntity = inconsistentEntity(entity.getDisplayName(), true);
        }

        dbEntity.setPriority(priority);
        getEbeanServer().save(dbEntity);
    }

    @Override
    protected void deleteRegions(Collection<PermissionRegion> regions) {
        boolean inconsistent = false;

        List<PermissionRegion> dbRegions = new ArrayList<PermissionRegion>(regions.size());
        for (PermissionRegion region : regions) {
            PermissionRegion dbRegion = getEbeanServer().find(PermissionRegion.class).where()
                    .eq("name", region.getName().toLowerCase())
                    .findUnique();
            if (dbRegion == null)
                inconsistent = true;
            else
                dbRegions.add(dbRegion);
        }

        if (inconsistent)
            databaseInconsistency();

        if (!dbRegions.isEmpty())
            getEbeanServer().delete(dbRegions);
    }

    @Override
    protected void deleteWorlds(Collection<PermissionWorld> worlds) {
        boolean inconsistent = false;

        List<PermissionWorld> dbWorlds = new ArrayList<PermissionWorld>(worlds.size());
        for (PermissionWorld world : worlds) {
            PermissionWorld dbWorld = getEbeanServer().find(PermissionWorld.class).where()
                    .eq("name", world.getName().toLowerCase())
                    .findUnique();
            if (dbWorld == null)
                inconsistent = true;
            else
                dbWorlds.add(dbWorld);
        }

        if (inconsistent)
            databaseInconsistency();

        if (!dbWorlds.isEmpty())
            getEbeanServer().delete(dbWorlds);
    }

    public void load() {
        List<PermissionEntity> players = getEbeanServer().createQuery(PermissionEntity.class,
                "find PermissionEntity fetch permissions where group = false")
                .findList();
        List<PermissionEntity> groups = getEbeanServer().createQuery(PermissionEntity.class,
                "find PermissionEntity fetch permissions fetch parent (displayName) fetch children fetch memberships where group = true")
                .findList();
        load(players, groups);
    }

    synchronized public void load(List<PermissionEntity> players, List<PermissionEntity> groups) {
        getPlayers().clear();
        getGroups().clear();
        getRegions().clear();
        getWorlds().clear();

        // Create full copies to force lazy-loads
        for (PermissionEntity player : players) {
            PermissionEntity newPlayer = getEntity(player.getDisplayName(), false, true);
            loadPermissions(player.getPermissions(), newPlayer);
        }
        for (PermissionEntity group : groups) {
            PermissionEntity newGroup = getEntity(group.getDisplayName(), true, true);
            loadPermissions(group.getPermissions(), newGroup);
            newGroup.setPriority(group.getPriority());
            if (group.getParent() != null) {
                PermissionEntity parentEntity = getEntity(group.getParent().getDisplayName(), true, true);
                newGroup.setParent(parentEntity);
                parentEntity.getChildren().add(newGroup);
            }
            for (Membership membership : group.getMemberships()) {
                Membership newMembership = new Membership();
                newMembership.setMember(membership.getMember());
                newMembership.setGroup(newGroup);
                newGroup.getMemberships().add(newMembership);
            }
        }
    }

    private void loadPermissions(Collection<Entry> permissions, PermissionEntity entity) {
        for (Entry entry : permissions) {
            Entry newEntry = new Entry();

            newEntry.setRegion(entry.getRegion() == null ? null : getRegion(entry.getRegion().getName(), true));
            newEntry.setWorld(entry.getWorld() == null ? null : getWorld(entry.getWorld().getName(), true));
            newEntry.setPermission(entry.getPermission());
            newEntry.setValue(entry.isValue());

            newEntry.setEntity(entity);
            entity.getPermissions().add(newEntry);
        }
    }

    private void databaseInconsistency() {
        logger.log(Level.WARNING, "Database inconsistency detected; please do a /permissions reload");
    }

    private PermissionEntity inconsistentEntity(String name, boolean group) {
        databaseInconsistency();
        PermissionEntity entity = new PermissionEntity();
        entity.setName(name.toLowerCase());
        entity.setGroup(group);
        entity.setDisplayName(name);
        getEbeanServer().save(entity);
        return entity;
    }

    private PermissionRegion inconsistentRegion(String name) {
        databaseInconsistency();
        PermissionRegion region = new PermissionRegion();
        region.setName(name.toLowerCase());
        getEbeanServer().save(region);
        return region;
    }

    private PermissionWorld inconsistentWorld(String name) {
        databaseInconsistency();
        PermissionWorld world = new PermissionWorld();
        world.setName(name.toLowerCase());
        getEbeanServer().save(world);
        return world;
    }

}
