package org.tyrannyofheaven.bukkit.zPermissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

public abstract class AbstractResolverTest {

    private static final String TEST_PLAYER = "Player";

    protected static final String TEST_GROUP1 = "Group1";

    private static final String TEST_GROUP2 = "Group2";

    private static final String TEST_WORLD1 = "WorldA";

    private static final String TEST_WORLD2 = "WorldB";

    private static final String TEST_REGION = "MyRegion";

    protected PermissionDao dao;

    protected PermissionsResolver resolver;

    protected abstract void begin();
    
    protected abstract void commit();
    
    protected abstract void end();

    PermissionDao getDao() {
        return dao;
    }

    PermissionsResolver getResolver() {
        return resolver;
    }

    private void assertPermission(Map<String, Boolean> permissions, String permission, boolean value) {
        Boolean check = permissions.get(permission);
        if (check == null) check = Boolean.FALSE;
        assertEquals(value, check);
    }

    private void assertPermission(Map<String, Boolean> permissions, String permission) {
        assertPermission(permissions, permission, true);
    }

    private void setPermissions(String name, boolean group, String... perms) {
        begin();
        try {
            for (String perm : perms) {
                WorldPermission wp = new WorldPermission(perm);
                getDao().setPermission(name, group, wp.getRegion(), wp.getWorld(), wp.getPermission(), true);
            }
            commit();
        }
        finally {
            end();
        }
    }

    private Map<String, Boolean> resolve(String player, String world, String... regions) {
        if (regions == null)
            regions = new String[0];
    
        // Resolve
        Map<String, Boolean> permissions;
        begin();
        try {
            Set<String> regionSet = new HashSet<String>();
            for (String region : regions) {
                regionSet.add(region.toLowerCase());
            }
            permissions = getResolver().resolvePlayer(player, world.toLowerCase(), regionSet);
            commit(); // even though read-only, this is what the plugin does
        }
        finally {
            end();
        }
        
        return permissions;
    }

    @Before
    public void setUp() {
        resolver.setIncludeDefaultInAssigned(true);
    }

    @After
    public void tearDown() {
        // Purge database
        begin();
        try {
            for (PermissionEntity entity : getDao().getEntities(false)) {
                getDao().deleteEntity(entity.getName(), false);
            }
            for (PermissionEntity entity : getDao().getEntities(true)) {
                getDao().deleteEntity(entity.getName(), true);
            }
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testBasicResolve() {
        setPermissions(TEST_PLAYER, false, "basic.perm");
    
        Map<String, Boolean> permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm");
    }

    @Test
    public void testBasicWorldResolve() {
        setPermissions(TEST_PLAYER, false,
                "basic.perm1",
                TEST_WORLD1 + ":basic.perm2",
                TEST_WORLD2 + ":basic.perm3");
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD2);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
    }

    @Test
    public void testBasicRegionResolve() {
        setPermissions(TEST_PLAYER, false,
                "basic.perm1",
                TEST_REGION + "/" + TEST_WORLD1 + ":basic.perm2",
                TEST_WORLD2 + ":basic.perm3");
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3", false);
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1, TEST_REGION);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD2);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD2, TEST_REGION);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
    }

    @Test
    public void testDefaultGroupResolve() {
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
        
        resolver.setIncludeDefaultInAssigned(false);
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2", false);
    }

    @Test
    public void testNoDefaultAssignedGroupResolve() {
        // Non-existent default group
        assertTrue(getDao().createGroup(TEST_GROUP2));
        
        begin();
        try {
            getDao().setGroup(TEST_PLAYER, TEST_GROUP2);
            commit();
        }
        finally {
            end ();
        }
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    
        // Shouldn't change outcome
        resolver.setIncludeDefaultInAssigned(false);
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testDefaultAssignedGroupResolve() {
        assertTrue(getDao().createGroup(TEST_GROUP1));
        assertTrue(getDao().createGroup(TEST_GROUP2));
        
        begin();
        try {
            getDao().setGroup(TEST_PLAYER, TEST_GROUP2);
            commit();
        }
        finally {
            end ();
        }
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
        
        // Explicitly add to default group
        begin();
        try {
            getDao().addMember(TEST_GROUP1, TEST_PLAYER);
            commit();
        }
        finally {
            end();
        }
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");
    
        resolver.setIncludeDefaultInAssigned(false);
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testDefaultAssignedGroupResolveInherited() {
        assertTrue(getDao().createGroup(TEST_GROUP1));
        assertTrue(getDao().createGroup(TEST_GROUP2));
        
        // Set up parent/child relationship
        begin();
        try {
            getDao().setParent(TEST_GROUP2, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }
        // Confirm
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, true);
        assertNotNull(entity);
        assertNotNull(entity.getParent());
        assertEquals(TEST_GROUP1, entity.getParent().getDisplayName());
    
        begin();
        try {
            getDao().setGroup(TEST_PLAYER, TEST_GROUP2);
            commit();
        }
        finally {
            end ();
        }
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    
        // Explicitly add to default group
        begin();
        try {
            getDao().addMember(TEST_GROUP1, TEST_PLAYER);
            commit();
        }
        finally {
            end();
        }
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");
    
        resolver.setIncludeDefaultInAssigned(false);
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testBasicGroupResolve() {
        setPermissions(TEST_PLAYER, false,
                "basic.perm1");
        assertTrue(getDao().createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, true,
                "basic.perm2");
        assertTrue(getDao().createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, true,
                "basic.perm3");
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2"); // default group
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
        
        begin();
        try {
            getDao().setGroup(TEST_PLAYER, TEST_GROUP2);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = getDao().getGroups(TEST_PLAYER);
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testBasicInheritResolve() {
        setPermissions(TEST_PLAYER, false,
                "basic.perm1");
        assertTrue(getDao().createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, true,
                "basic.perm2");
        assertTrue(getDao().createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, true,
                "basic.perm3");
    
        // Set up parent/child relationship
        begin();
        try {
            getDao().setParent(TEST_GROUP2, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }
        // Confirm
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, true);
        assertNotNull(entity);
        assertNotNull(entity.getParent());
        assertEquals(TEST_GROUP1, entity.getParent().getDisplayName());
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2"); // default group
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
    
        // Switch group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER, TEST_GROUP2);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = getDao().getGroups(TEST_PLAYER);
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
        
        // Switch back
        begin();
        try {
            getDao().removeMember(TEST_GROUP2, TEST_PLAYER);
            commit();
        }
        finally {
            end();
        }
        // Confirm
        groups = getDao().getGroups(TEST_PLAYER);
        assertTrue(groups.isEmpty());
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
    }

    @Test
    public void testInheritWorldResolve() {
        setPermissions(TEST_PLAYER, false,
            "basic.perm1");
        assertTrue(getDao().createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, true,
            TEST_WORLD1 + ":basic.perm2");
        assertTrue(getDao().createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, true,
            TEST_WORLD2 + ":basic.perm3");
    
        // Set up parent/child relationship
        begin();
        try {
            getDao().setParent(TEST_GROUP2, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }
        // Confirm
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, true);
        assertNotNull(entity);
        assertNotNull(entity.getParent());
        assertEquals(TEST_GROUP1, entity.getParent().getDisplayName());
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD2);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
    
        // Switch group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER, TEST_GROUP2);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = getDao().getGroups(TEST_PLAYER);
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    
        permissions = resolve(TEST_PLAYER, TEST_WORLD2);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

}
