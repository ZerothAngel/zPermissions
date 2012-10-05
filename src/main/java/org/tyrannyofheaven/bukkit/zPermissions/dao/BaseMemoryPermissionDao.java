package org.tyrannyofheaven.bukkit.zPermissions.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

/**
 * Base implementation of a fully in-memory DAO.
 * 
 * @author zerothangel
 */
public abstract class BaseMemoryPermissionDao implements PermissionDao {

    private final Map<String, PermissionRegion> regions = new HashMap<String, PermissionRegion>();

    private final Map<String, PermissionWorld> worlds = new HashMap<String, PermissionWorld>();

    private final Map<String, PermissionEntity> players = new HashMap<String, PermissionEntity>();

    private final Map<String, PermissionEntity> groups = new HashMap<String, PermissionEntity>();

    private final Map<String, Set<PermissionEntity>> reverseMembershipMap = new HashMap<String, Set<PermissionEntity>>();

    protected Map<String, PermissionRegion> getRegions() {
        return regions;
    }

    protected Map<String, PermissionWorld> getWorlds() {
        return worlds;
    }

    protected Map<String, PermissionEntity> getPlayers() {
        return players;
    }

    protected Map<String, PermissionEntity> getGroups() {
        return groups;
    }

    protected Map<String, Set<PermissionEntity>> getReverseMembershipMap() {
        return reverseMembershipMap;
    }

    protected PermissionRegion getRegion(String region, boolean create) {
        PermissionRegion permissionRegion = null;
        if (region != null) {
            region = region.toLowerCase();
            permissionRegion = regions.get(region);
            if (permissionRegion == null) {
                if (create) {
                    permissionRegion = new PermissionRegion();
                    permissionRegion.setName(region);
                    regions.put(region, permissionRegion);
                    createRegion(permissionRegion);
                }
                else {
                    throw new IllegalArgumentException("No such region");
                }
            }
        }
        return permissionRegion;
    }

    protected abstract void createRegion(PermissionRegion region);

    // For unit testing
    PermissionRegion getRegion(String region) {
        return regions.get(region.toLowerCase());
    }

    protected PermissionWorld getWorld(String world, boolean create) {
        PermissionWorld permissionWorld = null;
        if (world != null) {
            world = world.toLowerCase();
            permissionWorld = worlds.get(world);
            if (permissionWorld == null) {
                if (create) {
                    permissionWorld = new PermissionWorld();
                    permissionWorld.setName(world);
                    worlds.put(world, permissionWorld);
                    createWorld(permissionWorld);
                }
                else {
                    throw new IllegalArgumentException("No such world");
                }
            }
        }
        return permissionWorld;
    }

    protected abstract void createWorld(PermissionWorld world);

    // For unit testing
    PermissionWorld getWorld(String world) {
        return worlds.get(world.toLowerCase());
    }

    protected PermissionEntity getEntity(String name, boolean group, boolean create) {
        PermissionEntity entity;
        if (group)
            entity = groups.get(name.toLowerCase());
        else
            entity = players.get(name.toLowerCase());
        if (entity == null && create) {
            entity = new PermissionEntity();
            entity.setName(name.toLowerCase());
            entity.setGroup(group);
            entity.setDisplayName(name);
            if (group)
                groups.put(name.toLowerCase(), entity);
            else
                players.put(name.toLowerCase(), entity);
            createEntity(entity);
        }
        return entity;
    }

    protected abstract void createEntity(PermissionEntity entity);

    private PermissionEntity getGroup(String name) {
        PermissionEntity group = getEntity(name, true, false);
        if (group == null)
            throw new MissingGroupException(name);
        return group;
    }

    @Override
    public Boolean getPermission(String name, boolean group, String region, String world, String permission) {
        PermissionEntity entity = getEntity(name, group, false);
        if (entity == null)
            return null;
    
        PermissionRegion permissionRegion;
        try {
            permissionRegion = getRegion(region, false);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    
        PermissionWorld permissionWorld;
        try {
            permissionWorld = getWorld(world, false);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    
        for (Entry entry : entity.getPermissions()) {
            if (entry.getPermission().equalsIgnoreCase(permission) &&
                (permissionRegion == null ? entry.getRegion() == null : permissionRegion.equals(entry.getRegion())) &&
                (permissionWorld == null ? entry.getWorld() == null : permissionWorld.equals(entry.getWorld()))) {
                return entry.isValue();
            }
        }
        return null;
    }

    @Override
    public void setPermission(String name, boolean group, String region, String world, String permission, boolean value) {
        PermissionEntity owner;
        if (group) {
            owner = getGroup(name);
        }
        else {
            owner = getEntity(name, group, true);
        }
    
        PermissionRegion permissionRegion = getRegion(region, true);
    
        PermissionWorld permissionWorld = getWorld(world, true);
    
        permission = permission.toLowerCase();
    
        Entry found = null;
        for (Entry entry : owner.getPermissions()) {
            if (permission.equals(entry.getPermission()) &&
                    (permissionRegion == null ? entry.getRegion() == null : permissionRegion.equals(entry.getRegion())) &&
                    (permissionWorld == null ? entry.getWorld() == null : permissionWorld.equals(entry.getWorld()))) {
                found = entry;
                break;
            }
        }
    
        if (found == null) {
            found = new Entry();
            found.setEntity(owner);
            found.setRegion(permissionRegion);
            found.setWorld(permissionWorld);
            found.setPermission(permission);
            
            owner.getPermissions().add(found);
        }
    
        found.setValue(value);
        createOrUpdateEntry(found);
    }

    protected abstract void createOrUpdateEntry(Entry entry);

    @Override
    public boolean unsetPermission(String name, boolean group, String region, String world, String permission) {
        PermissionEntity entity = getEntity(name, group, false);
        if (entity == null)
            return false;
    
        PermissionRegion permissionRegion;
        try {
            permissionRegion = getRegion(region, false);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    
        PermissionWorld permissionWorld;
        try {
            permissionWorld = getWorld(world, false);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    
        for (Iterator<Entry> i = entity.getPermissions().iterator(); i.hasNext();) {
            Entry entry = i.next();
            if (entry.getPermission().equalsIgnoreCase(permission) &&
                    (permissionRegion == null ? entry.getRegion() == null : permissionRegion.equals(entry.getRegion())) &&
                    (permissionWorld == null ? entry.getWorld() == null : permissionWorld.equals(entry.getWorld()))) {
                i.remove();
                deleteEntry(entry);
                cleanWorldsAndRegions();
                return true;
            }
        }
        return false;
    }

    protected abstract void deleteEntry(Entry entry);

    @Override
    public void addMember(String groupName, String member) {
        PermissionEntity group = getGroup(groupName);
    
        for (Membership membership : group.getMemberships()) {
            if (membership.getMember().equalsIgnoreCase(member))
                return;
        }
    
        Membership membership = new Membership();
        membership.setMember(member.toLowerCase());
        membership.setGroup(group);
        
        group.getMemberships().add(membership);
        
        createMembership(membership);
        
        rememberMembership(group, membership);
    }

    protected abstract void createMembership(Membership membership);

    @Override
    public boolean removeMember(String groupName, String member) {
        PermissionEntity group = getGroup(groupName);
        
        for (Iterator<Membership> i = group.getMemberships().iterator(); i.hasNext();) {
            Membership membership = i.next();
            if (membership.getMember().equalsIgnoreCase(member)) {
                i.remove();
                deleteMembership(membership);
                forgetMembership(group, membership);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getGroups(String member) {
        List<PermissionEntity> groupsEntities = new ArrayList<PermissionEntity>();
        Set<PermissionEntity> groups = reverseMembershipMap.get(member.toLowerCase());
        if (groups != null)
            groupsEntities.addAll(groups);
    
        Collections.sort(groupsEntities, new Comparator<PermissionEntity>() {
            @Override
            public int compare(PermissionEntity a, PermissionEntity b) {
                int pri = a.getPriority() - b.getPriority();
                if (pri != 0)
                    return pri;
                return a.getName().compareTo(b.getName());
            }
        });
    
        List<String> resultString = new ArrayList<String>(groupsEntities.size());
        for (PermissionEntity group : groupsEntities) {
            resultString.add(group.getDisplayName());
        }
        return resultString;
    }

    @Override
    public List<String> getMembers(String group) {
        PermissionEntity groupEntity = getEntity(group, true, false);
        if (groupEntity == null)
            return new ArrayList<String>(); // compat with AvajePermissionDao
    
        List<String> result = new ArrayList<String>();
        for (Membership membership : groupEntity.getMemberships()) {
            result.add(membership.getMember());
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public PermissionEntity getEntity(String name, boolean group) {
        if (group)
            return groups.get(name.toLowerCase());
        else
            return players.get(name.toLowerCase());
    }

    @Override
    public List<PermissionEntity> getEntities(boolean group) {
        if (group)
            return new ArrayList<PermissionEntity>(groups.values());
        else
            return new ArrayList<PermissionEntity>(players.values());
    }

    @Override
    public void setGroup(String playerName, String groupName) {
        PermissionEntity group = getGroup(groupName);
    
        Membership found = null;
        Set<PermissionEntity> groups = reverseMembershipMap.get(playerName.toLowerCase());
        if (groups != null) {
            for (PermissionEntity groupEntity : groups) {
                for (Iterator<Membership> i = groupEntity.getMemberships().iterator(); i.hasNext();) {
                    Membership membership = i.next();
                    if (membership.getMember().equalsIgnoreCase(playerName)) {
                        if (!membership.getGroup().equals(group)) {
                            i.remove();
                            deleteMembership(membership);
                        }
                        else {
                            found = membership;
                        }
                        break;
                    }
                }
            }
        }
    
        if (found == null) {
            found = new Membership();
            found.setMember(playerName.toLowerCase());
            found.setGroup(group);
            
            group.getMemberships().add(found);
            
            createMembership(found);
        }
        
        reverseMembershipMap.remove(playerName.toLowerCase());
        rememberMembership(group, found);
    }

    @Override
    public void setParent(String groupName, String parentName) {
        PermissionEntity group = getGroup(groupName);
    
        if (parentName != null) {
            PermissionEntity parent = getGroup(parentName);
    
            // Check for a cycle
            PermissionEntity check = parent;
            while (check != null) {
                if (group.equals(check)) {
                    throw new DaoException("This would result in an inheritance cycle!");
                }
                check = check.getParent();
            }
    
            group.setParent(parent);
            parent.getChildren().add(group);
        }
        else {
            group.setParent(null);
        }
        
        setEntityParent(group, group.getParent());
    }

    protected abstract void setEntityParent(PermissionEntity entity, PermissionEntity parent);

    @Override
    public void setPriority(String groupName, int priority) {
        PermissionEntity group = getGroup(groupName);
    
        group.setPriority(priority);
        
        setEntityPriority(group, priority);
    }

    protected abstract void setEntityPriority(PermissionEntity entity, int priority);

    private void cleanWorldsAndRegions() {
        // Easier to just see what is used
        Set<PermissionRegion> usedRegions = new HashSet<PermissionRegion>();
        Set<PermissionWorld> usedWorlds = new HashSet<PermissionWorld>();
        
        List<PermissionEntity> entities = new ArrayList<PermissionEntity>();
        entities.addAll(groups.values());
        entities.addAll(players.values());
        
        for (PermissionEntity entity : entities) {
            for (Entry entry : entity.getPermissions()) {
                if (entry.getRegion() != null)
                    usedRegions.add(entry.getRegion());
                if (entry.getWorld() != null)
                    usedWorlds.add(entry.getWorld());
            }
        }
        
        // Determine what needs to be deleted
        Set<PermissionRegion> regionsToDelete = new HashSet<PermissionRegion>(regions.values());
        regionsToDelete.removeAll(usedRegions);
        Set<PermissionWorld> worldsToDelete = new HashSet<PermissionWorld>(worlds.values());
        worldsToDelete.removeAll(usedWorlds);
        
        // Re-build lists
        regions.clear();
        for (PermissionRegion region : usedRegions) {
            regions.put(region.getName(), region);
        }
        worlds.clear();
        for (PermissionWorld world : usedWorlds) {
            worlds.put(world.getName(), world);
        }
        
        // Tell underlying DAO about deleted regions/worlds
        if (!regionsToDelete.isEmpty())
            deleteRegions(regionsToDelete);
        if (!worldsToDelete.isEmpty())
            deleteWorlds(worldsToDelete);
    }

    protected abstract void deleteRegions(Collection<PermissionRegion> regions);
    
    protected abstract void deleteWorlds(Collection<PermissionWorld> worlds);

    @Override
    public boolean deleteEntity(String name, boolean group) {
        PermissionEntity entity = getEntity(name, group, false);
        
        if (group) {
            // Deleting a group
            if (entity != null) {
                // Break parent/child relationship
                for (PermissionEntity child : entity.getChildren()) {
                    child.setParent(null);
                }
    
                // Delete group's entity
                groups.remove(entity.getName().toLowerCase());
                deleteEntity(entity);
                cleanWorldsAndRegions();
                forgetMembershipGroup(entity);
                return true;
            }
        }
        else {
            // Deleting a player
    
            boolean found = false;
    
            // Delete memberships
            Set<PermissionEntity> groups = reverseMembershipMap.get(name.toLowerCase());
            if (groups != null) {
                for (PermissionEntity groupEntity : groups) {
                    for (Iterator<Membership> i = groupEntity.getMemberships().iterator(); i.hasNext();) {
                        Membership membership = i.next();
                        if (membership.getMember().equalsIgnoreCase(name)) {
                            i.remove();
                            deleteMembership(membership);
                            found = true;
                            break;
                        }
                    }
                }
    
                reverseMembershipMap.remove(name.toLowerCase());
            }

            if (entity != null) {
                // Delete player's entity
                players.remove(entity.getName().toLowerCase());
                deleteEntity(entity);
                cleanWorldsAndRegions();
            }
            
            return found || entity != null;
        }
        
        return false; // nothing to delete
    }

    protected abstract void deleteEntity(PermissionEntity entity);
    
    protected abstract void deleteMembership(Membership membership);

    @Override
    public List<String> getAncestry(String groupName) {
        PermissionEntity group = getEntity(groupName, true, false);
        if (group == null) // NB only time this will be null is if the default group doesn't exist
            return new ArrayList<String>();
    
        // Build list of group ancestors
        List<String> ancestry = new ArrayList<String>();
        ancestry.add(group.getDisplayName());
        while (group.getParent() != null) {
            group = group.getParent();
            ancestry.add(group.getDisplayName());
        }
        
        // Reverse list (will be applying farthest ancestors first)
        Collections.reverse(ancestry);
    
        return ancestry;
    }

    @Override
    public List<Entry> getEntries(String name, boolean group) {
        PermissionEntity entity = getEntity(name, group, false);
        if (entity == null) // NB special consideration for non-existent default group
            return Collections.emptyList();
    
        return new ArrayList<Entry>(entity.getPermissions());
    }

    @Override
    public boolean createGroup(String name) {
        PermissionEntity group = getEntity(name, true, false); // so we know it was created
        if (group == null) {
            group = getEntity(name, true, true);
            return true;
        }
        else
            return false;
    }

    @Override
    public List<String> getEntityNames(boolean group) {
        Collection<PermissionEntity> entities;
        if (group)
            entities = groups.values();
        else
            entities = players.values();
        List<String> result = new ArrayList<String>(entities.size());
        for (PermissionEntity entity : entities) {
            result.add(entity.getDisplayName());
        }
        return result;
    }

    protected void rememberMembership(PermissionEntity group, Membership membership) {
        Set<PermissionEntity> groups = reverseMembershipMap.get(membership.getMember());
        if (groups == null) {
            groups = new HashSet<PermissionEntity>();
            reverseMembershipMap.put(membership.getMember(), groups);
        }
        groups.add(group);
    }

    private void forgetMembership(PermissionEntity group, Membership membership) {
        Set<PermissionEntity> groups = reverseMembershipMap.get(membership.getMember());
        if (groups != null)
            groups.remove(group);
    }

    private void forgetMembershipGroup(PermissionEntity group) {
        for (Set<PermissionEntity> groups : reverseMembershipMap.values()) {
            groups.remove(group);
        }
    }

}
