package org.tyrannyofheaven.bukkit.zPermissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

public abstract class AbstractResolverTest {

    private static final String TEST_PLAYER = "Player";

    private static final UUID TEST_PLAYER_UUID = UUID.randomUUID();

    protected static final String TEST_GROUP1 = "Group1";

    private static final String TEST_GROUP2 = "Group2";

    private static final String TEST_GROUP3 = "Group3";

    private static final String TEST_WORLD1 = "WorldA";

    private static final String TEST_WORLD2 = "WorldB";

    private static final String TEST_REGION1 = "MyRegion1";

    private static final String TEST_REGION2 = "MyRegion2";

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
        Boolean check = permissions.get(permission.toLowerCase());
        if (check == null) check = Boolean.FALSE;
        assertEquals(value, check);
    }

    private void assertPermission(Map<String, Boolean> permissions, String permission) {
        assertPermission(permissions, permission, true);
    }

    private void setPermissions(String name, UUID uuid, boolean group, String... perms) {
        begin();
        try {
            for (String perm : perms) {
                QualifiedPermission wp = new QualifiedPermission(perm);
                getDao().setPermission(name, uuid, group, wp.getRegion(), wp.getWorld(), wp.getPermission(), true);
            }
            commit();
        }
        finally {
            end();
        }
    }

    private void setPermissionsFalse(String name, UUID uuid, boolean group, String... perms) {
        begin();
        try {
            for (String perm : perms) {
                QualifiedPermission wp = new QualifiedPermission(perm);
                getDao().setPermission(name, uuid, group, wp.getRegion(), wp.getWorld(), wp.getPermission(), false);
            }
            commit();
        }
        finally {
            end();
        }
    }

    private boolean createGroup(String name) {
        begin();
        try {
            boolean result = getDao().createGroup(name);
            commit();
            return result;
        }
        finally {
            end();
        }
    }

    private Map<String, Boolean> resolve(String player, UUID uuid, String world, String... regions) {
        if (regions == null)
            regions = new String[0];
    
        // Resolve
        Map<String, Boolean> permissions;
        begin();
        try {
            Set<String> regionSet = new LinkedHashSet<>();
            for (String region : regions) {
                regionSet.add(region.toLowerCase());
            }
            permissions = getResolver().resolvePlayer(uuid, world.toLowerCase(), regionSet).getPermissions();
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
        resolver.setInterleavedPlayerPermissions(true);
        resolver.clearWorldAliases();
    }

    @After
    public void tearDown() {
        // Purge database
        begin();
        try {
            for (PermissionEntity entity : getDao().getEntities(false)) {
                getDao().deleteEntity(entity.getDisplayName(), entity.getUuid(), false);
            }
            for (PermissionEntity entity : getDao().getEntities(true)) {
                getDao().deleteEntity(entity.getName(), null, true);
            }
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testBasicResolve() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID, false, "basic.perm");
    
        Map<String, Boolean> permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm");
    }

    @Test
    public void testBasicWorldResolve() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false,
                "basic.perm1",
                TEST_WORLD1 + ":basic.perm2", TEST_WORLD2 + ":basic.perm3");
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
    }

    @Test
    public void testBasicRegionResolve() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false,
                "basic.perm1",
                TEST_REGION1 + "/" + TEST_WORLD1 + ":basic.perm2", TEST_WORLD2 + ":basic.perm3");
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3", false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2, TEST_REGION1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
    }

    @Test
    public void testDefaultGroupResolve() {
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
        
        resolver.setIncludeDefaultInAssigned(false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2", false);
    }

    @Test
    public void testNoDefaultAssignedGroupResolve() {
        // Non-existent default group
        assertTrue(createGroup(TEST_GROUP2));
        
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    
        // Shouldn't change outcome
        resolver.setIncludeDefaultInAssigned(false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testDefaultAssignedGroupResolve() {
        assertTrue(createGroup(TEST_GROUP1));
        assertTrue(createGroup(TEST_GROUP2));
        
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
        
        // Explicitly add to default group
        begin();
        try {
            getDao().addMember(TEST_GROUP1, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end();
        }
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");
    
        resolver.setIncludeDefaultInAssigned(false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testDefaultAssignedGroupResolveInherited() {
        assertTrue(createGroup(TEST_GROUP1));
        assertTrue(createGroup(TEST_GROUP2));
        
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
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, null, true);
        assertNotNull(entity);
        assertFalse(entity.getParents().isEmpty());
        assertEquals(TEST_GROUP1, entity.getParents().get(0).getDisplayName());
    
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    
        // Explicitly add to default group
        begin();
        try {
            getDao().addMember(TEST_GROUP1, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end();
        }
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");
    
        resolver.setIncludeDefaultInAssigned(false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testBasicGroupResolve() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, null,
                true, "basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, null,
                true, "basic.perm3");
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2"); // default group
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
        
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
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
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, null,
                true, "basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, null,
                true, "basic.perm3");
    
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
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, null, true);
        assertNotNull(entity);
        assertFalse(entity.getParents().isEmpty());
        assertEquals(TEST_GROUP1, entity.getParents().get(0).getDisplayName());
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
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
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
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
            getDao().removeMember(TEST_GROUP2, TEST_PLAYER_UUID);
            commit();
        }
        finally {
            end();
        }
        // Confirm
        groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertTrue(groups.isEmpty());
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
    }

    @Test
    public void testInheritWorldResolve() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
            false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, null,
            true, TEST_WORLD1 + ":basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, null,
            true, TEST_WORLD2 + ":basic.perm3");
    
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
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, null, true);
        assertNotNull(entity);
        assertFalse(entity.getParents().isEmpty());
        assertEquals(TEST_GROUP1, entity.getParents().get(0).getDisplayName());
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        
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
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testInheritDifferentScopeResolve() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissionsFalse(TEST_GROUP1, null,
                true, TEST_WORLD2 + ":basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, null,
                true, "basic.perm2");
    
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
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, null, true);
        assertNotNull(entity);
        assertFalse(entity.getParents().isEmpty());
        assertEquals(TEST_GROUP1, entity.getParents().get(0).getDisplayName());
    
        // Switch group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");

        // Other world
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    // Same idea, but permission is swapped. Should work under old resolver.
    @Test
    public void testInheritDifferentScopeResolve2() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, null,
                true, "basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissionsFalse(TEST_GROUP2, null,
                true, TEST_WORLD2 + ":basic.perm2");
    
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
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, null, true);
        assertNotNull(entity);
        assertFalse(entity.getParents().isEmpty());
        assertEquals(TEST_GROUP1, entity.getParents().get(0).getDisplayName());
    
        // Switch group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");

        // Other world
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testAssignDifferentScopeResolve() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissionsFalse(TEST_GROUP1, null,
                true, TEST_WORLD2 + ":basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, null,
                true, "basic.perm2");
    
        // Set up priorities
        begin();
        try {
            getDao().setPriority(TEST_GROUP1, 100);
            commit();
        }
        finally {
            end();
        }
    
        // Add groups
        begin();
        try {
            getDao().addMember(TEST_GROUP1, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end ();
        }
        begin();
        try {
            getDao().addMember(TEST_GROUP2, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(2, groups.size());
        assertTrue(groups.contains(TEST_GROUP1));
        assertTrue(groups.contains(TEST_GROUP2));
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");

        // Other world
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");
    }

    // Same thing, but swapped priorities. Will fail unless precedence
    // resolution is global.
    @Test
    public void testAssignDifferentScopeResolve2() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissionsFalse(TEST_GROUP1, null,
                true, TEST_WORLD2 + ":basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, null,
                true, "basic.perm2");
    
        // Set up priorities
        begin();
        try {
            getDao().setPriority(TEST_GROUP2, 100);
            commit();
        }
        finally {
            end();
        }
    
        // Add groups
        begin();
        try {
            getDao().addMember(TEST_GROUP1, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end ();
        }
        begin();
        try {
            getDao().addMember(TEST_GROUP2, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(2, groups.size());
        assertTrue(groups.contains(TEST_GROUP1));
        assertTrue(groups.contains(TEST_GROUP2));
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");

        // Other world
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");
    }

    @Test
    public void testBasicTripleInheritResolve() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, null,
                true, "basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, null,
                true, "basic.perm3");
        assertTrue(createGroup(TEST_GROUP3));
        setPermissions(TEST_GROUP3, null,
                true, "basic.perm4");
    
        // Set up parent/child relationship
        begin();
        try {
            getDao().setParent(TEST_GROUP2, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }
        begin();
        try {
            getDao().setParent(TEST_GROUP3, TEST_GROUP2);
            commit();
        }
        finally {
            end();
        }
        // Confirm
        PermissionEntity entity = getDao().getEntity(TEST_GROUP2, null, true);
        assertNotNull(entity);
        assertFalse(entity.getParents().isEmpty());
        assertEquals(TEST_GROUP1, entity.getParents().get(0).getDisplayName());
    
        entity = getDao().getEntity(TEST_GROUP3, null, true);
        assertNotNull(entity);
        assertFalse(entity.getParents().isEmpty());
        assertEquals(TEST_GROUP2, entity.getParents().get(0).getDisplayName());

        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2"); // default group
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "basic.perm4", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "group.Group3", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
        assertPermission(permissions, "assignedgroup.Group3", false);
    
        // Switch group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3");
        assertPermission(permissions, "basic.perm4", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "group.Group3", false);
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2");
        assertPermission(permissions, "assignedgroup.Group3", false);
        
        // Switch to inner-most group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP3, null);
            commit();
        }
        finally {
            end();
        }
        // Confirm
        groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP3, groups.get(0));
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3");
        assertPermission(permissions, "basic.perm4");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "group.Group3");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2", false);
        assertPermission(permissions, "assignedgroup.Group3");
    }

    @Test
    public void testGroupExpiration() {
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP1));
        setPermissions(TEST_GROUP1, null,
                true, "basic.perm2");
        assertTrue(createGroup(TEST_GROUP2));
        setPermissions(TEST_GROUP2, null,
                true, "basic.perm3");
        assertTrue(createGroup(TEST_GROUP3));
        setPermissions(TEST_GROUP3, null,
                true, "basic.perm4");
        
        // Add permanent groups
        begin();
        try {
            getDao().addMember(TEST_GROUP1, TEST_PLAYER_UUID, TEST_PLAYER, null);
            getDao().addMember(TEST_GROUP2, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end();
        }
        
        Date now = new Date();

        // Add non-expired group
        begin();
        try {
            getDao().addMember(TEST_GROUP3, TEST_PLAYER_UUID, TEST_PLAYER, new Date(now.getTime() + 10000L));
            commit();
        }
        finally {
            end();
        }
        
        // DAO should return raw groups
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(3, groups.size());
        assertEquals(TEST_GROUP3, groups.get(2));

        // Should be resolved
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3");
        assertPermission(permissions, "basic.perm4");
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "group.Group3");
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");
        assertPermission(permissions, "assignedgroup.Group3");
        
        // Switch it to expired
        begin();
        try {
            getDao().addMember(TEST_GROUP3, TEST_PLAYER_UUID, TEST_PLAYER, new Date(now.getTime() - 10000L));
            commit();
        }
        finally {
            end();
        }
        
        // DAO should return raw groups
        groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(3, groups.size());
        assertEquals(TEST_GROUP3, groups.get(2));

        // Should no longer be resolved
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3");
        assertPermission(permissions, "basic.perm4", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "group.Group3", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2");
        assertPermission(permissions, "assignedgroup.Group3", false);
        
        // Change to single, non-expired group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP3, new Date(now.getTime() + 10000L));
            commit();
        }
        finally {
            end();
        }

        // DAO should return raw groups
        groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP3, groups.get(0));

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "basic.perm4");
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "group.Group3");
        assertPermission(permissions, "assignedgroup.Group1", false);
        assertPermission(permissions, "assignedgroup.Group2", false);
        assertPermission(permissions, "assignedgroup.Group3");

        // Change to single, expired group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP3, new Date(now.getTime() - 10000L));
            commit();
        }
        finally {
            end();
        }

        // DAO should return raw groups
        groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP3, groups.get(0));

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2"); // NB default group
        assertPermission(permissions, "basic.perm3", false);
        assertPermission(permissions, "basic.perm4", false);
        assertPermission(permissions, "group.Group1");
        assertPermission(permissions, "group.Group2", false);
        assertPermission(permissions, "group.Group3", false);
        assertPermission(permissions, "assignedgroup.Group1");
        assertPermission(permissions, "assignedgroup.Group2", false);
        assertPermission(permissions, "assignedgroup.Group3", false);
    }

    @Test
    public void testQualifierPrecedence() {
        setPermissionsFalse(TEST_PLAYER, TEST_PLAYER_UUID,
                false,
                TEST_WORLD2 + ":basic.perm1",
                TEST_REGION1 + "/basic.perm2", TEST_REGION1 + "/" + TEST_WORLD2 + ":basic.perm3");
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false,
                "basic.perm1",
                TEST_WORLD2 + ":basic.perm2", TEST_REGION1 + "/basic.perm3");

        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3", false);
        
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3");
        
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        
        assertPermission(permissions, "basic.perm1", false);
        assertPermission(permissions, "basic.perm2");
        assertPermission(permissions, "basic.perm3", false);

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2, TEST_REGION1);
        
        assertPermission(permissions, "basic.perm1", false);
        assertPermission(permissions, "basic.perm2", false);
        assertPermission(permissions, "basic.perm3", false);
    }

    // Test with interleave true (default)
    @Test
    public void testPlayerInterleave() {
        assertTrue(createGroup(TEST_GROUP1));
        setPermissionsFalse(TEST_GROUP1, null,
                true,
                TEST_WORLD2 + ":basic.perm1",
                TEST_REGION1 + "/basic.perm2", TEST_REGION1 + "/" + TEST_WORLD2 + ":basic.perm3");
        setPermissions(TEST_GROUP1, null,
                true,
                "basic.perm1",
                TEST_WORLD2 + ":basic.perm2", TEST_REGION1 + "/basic.perm3");

        // Set group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP1, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP1, groups.get(0));

        Map<String, Boolean> permissions;

        // Same level
        setPermissionsFalse(TEST_PLAYER, TEST_PLAYER_UUID, false, "basic.perm1");
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        assertPermission(permissions, "basic.perm1", false);
        
        // More specific level
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        assertPermission(permissions, "basic.perm1", false);
        
        // Should not make a difference (overridden by more specific level)
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID, false, "basic.perm1");
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        assertPermission(permissions, "basic.perm1", false);
    }

    // Now with interleave false
    @Test
    public void testPlayerInterleave2() {
        assertTrue(createGroup(TEST_GROUP1));
        setPermissionsFalse(TEST_GROUP1, null,
                true,
                TEST_WORLD2 + ":basic.perm1",
                TEST_REGION1 + "/basic.perm2", TEST_REGION1 + "/" + TEST_WORLD2 + ":basic.perm3");
        setPermissions(TEST_GROUP1, null,
                true,
                "basic.perm1",
                TEST_WORLD2 + ":basic.perm2", TEST_REGION1 + "/basic.perm3");

        // Set group
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP1, null);
            commit();
        }
        finally {
            end ();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(1, groups.size());
        assertEquals(TEST_GROUP1, groups.get(0));

        Map<String, Boolean> permissions;

        resolver.setInterleavedPlayerPermissions(false);

        // Same level
        setPermissionsFalse(TEST_PLAYER, TEST_PLAYER_UUID, false, "basic.perm1");
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        assertPermission(permissions, "basic.perm1", false);
        
        // More specific level (no change)
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        assertPermission(permissions, "basic.perm1", false);
        
        // Player-specific should override despite specific level
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID, false, "basic.perm1");
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        assertPermission(permissions, "basic.perm1");
    }

    @Test
    public void testAutoOverride() {
        // Set up groups
        assertTrue(createGroup(TEST_GROUP1));
        assertTrue(createGroup(TEST_GROUP2));
        setPermissionsFalse(TEST_GROUP2, null,
                true, "group." + TEST_GROUP1);
        setPermissions(TEST_GROUP2, null,
                true, "basic.perm1");
        assertTrue(createGroup(TEST_GROUP3));
        setPermissionsFalse(TEST_GROUP3, null,
                true, "group." + TEST_GROUP1);
        setPermissions(TEST_GROUP3, null,
                true, "basic.perm2");
        
        // Set inheritance
        begin();
        try {
            getDao().setParent(TEST_GROUP2, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }
        begin();
        try {
            getDao().setParent(TEST_GROUP3, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }

        // Set groups
        begin();
        try {
            getDao().addMember(TEST_GROUP2, TEST_PLAYER_UUID, TEST_PLAYER, null);
            getDao().addMember(TEST_GROUP3, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end();
        }
        // Confirm
        List<String> groups = Utils.toGroupNames(getDao().getGroups(TEST_PLAYER_UUID));
        assertEquals(2, groups.size());
        assertEquals(TEST_GROUP2, groups.get(0));
        assertEquals(TEST_GROUP3, groups.get(1));

        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        
        assertPermission(permissions, "group.Group1", false);
        assertPermission(permissions, "group.Group2");
        assertPermission(permissions, "group.Group3");
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
    }

    @Test
    public void testRegionOrdering() {
        setPermissionsFalse(TEST_PLAYER, TEST_PLAYER_UUID,
                false, TEST_REGION2 + "/basic.perm2");
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false,
                "basic.perm1", TEST_REGION1 + "/basic.perm2");
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION2);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION1, TEST_REGION2);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION2, TEST_REGION1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
    }

    @Test
    public void testRegionWorldOrdering() {
        // Same as above, but with world
        setPermissionsFalse(TEST_PLAYER, TEST_PLAYER_UUID,
                false, TEST_REGION2 + "/" + TEST_WORLD1 + ":basic.perm2");
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false,
                "basic.perm1", TEST_REGION1 + "/" + TEST_WORLD1 + ":basic.perm2");
    
        Map<String, Boolean> permissions;
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION1);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
    
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION2);
    
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION1, TEST_REGION2);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1, TEST_REGION2, TEST_REGION1);
        
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
    }

    private void setMetadata(String name, UUID uuid, boolean group, String metadataName, Object value) {
        begin();
        try {
            getDao().setMetadata(name, uuid, group, metadataName, value);
            commit();
        }
        finally {
            end();
        }
    }

    private Map<String, Object> resolveMetadata(String name, UUID uuid, boolean group) {
        begin();
        try {
            Map<String, Object> result;
            if (group)
                result = getResolver().resolveGroupMetadata(name).getMetadata();
            else
                result = getResolver().resolvePlayerMetadata(uuid).getMetadata();
            commit();
            return result;
        }
        finally {
            end();
        }
    }

    @Test
    public void testPlayerMetadataBasic() {
        setMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, "Test.Meta", "foo");
        
        Map<String, Object> metadata;
        metadata = resolveMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false);
        
        assertEquals(1, metadata.size());
        assertNotNull(metadata.get("test.meta")); // NB diff case as well
        assertEquals("foo", metadata.get("test.meta"));
    }

    @Test
    public void testGroupMetadataBasic() {
        assertTrue(createGroup(TEST_GROUP1));
        setMetadata(TEST_GROUP1, null, true, "Test.Meta", "bar");

        Map<String, Object> metadata;
        metadata = resolveMetadata(TEST_GROUP1, null, true);
        
        assertEquals(1, metadata.size());
        assertNotNull(metadata.get("test.meta")); // NB diff case as well
        assertEquals("bar", metadata.get("test.meta"));
    }
    
    @Test
    public void testGroupMetadataInheritance() {
        assertTrue(createGroup(TEST_GROUP1));
        setMetadata(TEST_GROUP1, null, true, "Test.Meta", "bar");
        setMetadata(TEST_GROUP1, null, true, "Test.MetaToo", "foo");

        assertTrue(createGroup(TEST_GROUP2));
        setMetadata(TEST_GROUP2, null, true, "Test.MetaToo", "bar");
        
        begin();
        try {
            getDao().setParent(TEST_GROUP2, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }

        Map<String, Object> metadata;
        metadata = resolveMetadata(TEST_GROUP1, null, true);
        
        assertEquals(2, metadata.size());
        assertEquals("bar", metadata.get("test.meta"));
        assertEquals("foo", metadata.get("test.metatoo"));
        
        metadata = resolveMetadata(TEST_GROUP2, null, true);
        
        assertEquals(2, metadata.size());
        assertEquals("bar", metadata.get("test.meta"));
        assertEquals("bar", metadata.get("test.metatoo"));
    }

    @Test
    public void testPlayerMetadataInheritance() {
        assertTrue(createGroup(TEST_GROUP1));
        setMetadata(TEST_GROUP1, null, true, "Test.Meta", "bar");
        setMetadata(TEST_GROUP1, null, true, "Test.MetaToo", "foo");

        assertTrue(createGroup(TEST_GROUP2));
        setMetadata(TEST_GROUP2, null, true, "Test.MetaToo", "bar");
        
        begin();
        try {
            getDao().setParent(TEST_GROUP2, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }

        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP1, null);
            commit();
        }
        finally {
            end();
        }

        Map<String, Object> metadata;
        metadata = resolveMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false);
        
        assertEquals(2, metadata.size());
        assertEquals("bar", metadata.get("test.meta"));
        assertEquals("foo", metadata.get("test.metatoo"));
        
        // Re-assign to group 2
        begin();
        try {
            getDao().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP2, null);
            commit();
        }
        finally {
            end();
        }
        
        metadata = resolveMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false);
        
        assertEquals(2, metadata.size());
        assertEquals("bar", metadata.get("test.meta"));
        assertEquals("bar", metadata.get("test.metatoo"));
        
        // Override a value
        setMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, "test.meta", "garply");
        // And set a new value
        setMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, "Test.MetaThree", "baz");

        metadata = resolveMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false);
        
        assertEquals(3, metadata.size());
        assertEquals("garply", metadata.get("test.meta"));
        assertEquals("bar", metadata.get("test.metatoo"));
        assertEquals("baz", metadata.get("test.metathree"));
    }

    @Test
    public void testWorldAlias() {
        // First no mirroring
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, TEST_WORLD1 + ":basic.perm1");
        setPermissions(TEST_PLAYER, TEST_PLAYER_UUID,
                false, TEST_WORLD2 + ":basic.perm2");
        
        Map<String, Boolean> permissions;

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        assertPermission(permissions, "basic.perm1", false);
        assertPermission(permissions, "basic.perm2");
        
        // Now make TEST_WORLD2 a mirror of TEST_WORLD1
        getResolver().addWorldAlias(TEST_WORLD2, TEST_WORLD1);

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        
        // Override basic.perm1 in TEST_WORLD2
        setPermissionsFalse(TEST_PLAYER, TEST_PLAYER_UUID,
                false, TEST_WORLD2 + ":basic.perm1");

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2", false);
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        assertPermission(permissions, "basic.perm1", false);
        assertPermission(permissions, "basic.perm2");

        // Flip the mirror
        getResolver().clearWorldAliases();
        getResolver().addWorldAlias(TEST_WORLD1, TEST_WORLD2);

        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD1);
        assertPermission(permissions, "basic.perm1");
        assertPermission(permissions, "basic.perm2");
        permissions = resolve(TEST_PLAYER, TEST_PLAYER_UUID, TEST_WORLD2);
        assertPermission(permissions, "basic.perm1", false);
        assertPermission(permissions, "basic.perm2");
    }

}
