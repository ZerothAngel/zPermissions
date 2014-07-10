/*
 * Copyright 2011, 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.zPermissions.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;
import org.tyrannyofheaven.bukkit.zPermissions.util.Utils;

public abstract class AbstractDaoTest {

    private static final String TEST_PLAYER = "Player";

    private static final UUID TEST_PLAYER_UUID = UUID.randomUUID();

    private static final String TEST_GROUP1 = "Group1";

    private static final String TEST_GROUP2 = "Group2";

    private static final String TEST_PERMISSION = "foo.bar";

    private static final String TEST_METADATA = "my_metadata";

    private static final String TEST_STRING_VALUE = "blahblahblah";
    
    private static final Long TEST_INT_VALUE = 1234567890L;

    private static final Double TEST_REAL_VALUE = 3.1415926535;
    
    private final InMemoryPermissionService permissionService = new InMemoryPermissionService();

    protected InMemoryPermissionService getPermissionService() {
        return permissionService;
    }

    protected abstract void begin();
    
    protected abstract void commit();
    
    protected abstract void end();

    protected abstract PermissionWorld getWorld(String name);

    protected abstract PermissionRegion getRegion(String name);

    @Test
    public void testGetSetUnset() {
        begin();
        try {
            // Null get
            assertNull(getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION));
    
            // Missing unset
            assertFalse(getPermissionService().unsetPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION));
    
            // Set something
            getPermissionService().setPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION, true);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION));
    
            // Set to something else
            getPermissionService().setPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION, false);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Verify it changed
            assertEquals(Boolean.FALSE, getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION));
    
            // Unset
            assertTrue(getPermissionService().unsetPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION));
            commit();
        }
        finally {
            end();
        }
            
        begin();
        try {
            // Verify it's gone
            assertNull(getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION));
            
            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_PLAYER, TEST_PLAYER_UUID, false));
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testWorldPermission() {
        final String TEST_WORLD = "myworld";
        
        begin();
        try {
            // Confirm missing world-specific permission
            assertNull(getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Set it
            getPermissionService().setPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, TEST_WORLD, TEST_PERMISSION, true);
            commit();
        }
        finally {
            end();
        }
        
        begin();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Peek into world table, make sure it's there
            assertNotNull(getWorld(TEST_WORLD));
            
            // Unset
            assertTrue(getPermissionService().unsetPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, TEST_WORLD, TEST_PERMISSION));
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Make sure it's gone
            assertNull(getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Should be gone from world table too
            assertNull(getWorld(TEST_WORLD));

            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_PLAYER, TEST_PLAYER_UUID, false));
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testRegionPermission() {
        // Regions are just like worlds
        final String TEST_REGION = "myregion";
        
        begin();
        try {
            // Confirm missing world-specific permission
            assertNull(getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Set it
            getPermissionService().setPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_REGION, null, TEST_PERMISSION, true);
            commit();
        }
        finally {
            end();
        }
        
        begin();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Peek into region table, make sure it's there
            assertNotNull(getRegion(TEST_REGION));
            
            // Unset
            assertTrue(getPermissionService().unsetPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_REGION, null, TEST_PERMISSION));
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Make sure it's gone
            assertNull(getPermissionService().getPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Should be gone from region table too
            assertNull(getRegion(TEST_REGION));

            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_PLAYER, TEST_PLAYER_UUID, false));
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testMembership() {
        begin();
        try {
            // Should not be in any groups
            assertTrue(getPermissionService().getGroups(TEST_PLAYER_UUID).isEmpty());
            
            // Add to a group
            assertTrue(getPermissionService().createGroup(TEST_GROUP1));
            getPermissionService().addMember(TEST_GROUP1, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm membership
            List<String> groups = Utils.toGroupNames(getPermissionService().getGroups(TEST_PLAYER_UUID));
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP1, groups.get(0));
            
            List<String> players = Utils.toMembers(getPermissionService().getMembers(TEST_GROUP1), false);
            assertEquals(1, players.size());
            assertTrue(players.contains(TEST_PLAYER));
    
            // Add to second group
            assertTrue(getPermissionService().createGroup(TEST_GROUP2));
            getPermissionService().addMember(TEST_GROUP2, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm membership
            List<String> groups = Utils.toGroupNames(getPermissionService().getGroups(TEST_PLAYER_UUID));
            assertEquals(2, groups.size());
            // NB: When priorities are equal, falls back to lexicographical ordering
            assertEquals(TEST_GROUP1, groups.get(0));
            assertEquals(TEST_GROUP2, groups.get(1));
            
            List<String> players = Utils.toMembers(getPermissionService().getMembers(TEST_GROUP2), false);
            assertEquals(1, players.size());
            assertTrue(players.contains(TEST_PLAYER));
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Give TEST_GROUP1 a higher priority
            getPermissionService().setPriority(TEST_GROUP1, 100);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm new ordering
            List<String> groups = Utils.toGroupNames(getPermissionService().getGroups(TEST_PLAYER_UUID));
            assertEquals(2, groups.size());
            assertEquals(TEST_GROUP2, groups.get(0));
            assertEquals(TEST_GROUP1, groups.get(1));
            
            // Remove membership from TEST_GROUP1
            assertTrue(getPermissionService().removeMember(TEST_GROUP1, TEST_PLAYER_UUID));
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm membership
            List<String> groups = Utils.toGroupNames(getPermissionService().getGroups(TEST_PLAYER_UUID));
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP2, groups.get(0));
    
            assertTrue(getPermissionService().getMembers(TEST_GROUP1).isEmpty());
    
            // Set group
            getPermissionService().setGroup(TEST_PLAYER_UUID, TEST_PLAYER, TEST_GROUP1, null);
            commit();
        }
        finally {
            end();
        }
            
        begin();
        try {
            // Confirm membership
            List<String> groups = Utils.toGroupNames(getPermissionService().getGroups(TEST_PLAYER_UUID));
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP1, groups.get(0));
            
            // Purge final group
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP1, null, true));
            commit();
        }
        finally {
            end();
        }
        
        begin();
        try {
            // Confirm
            List<String> groups = Utils.toGroupNames(getPermissionService().getGroups(TEST_PLAYER_UUID));
            assertTrue(groups.isEmpty());

            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP2, null, true));
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testMembershipPlayerDelete() {
        begin();
        try {
            // Should not be in any groups
            assertTrue(getPermissionService().getGroups(TEST_PLAYER_UUID).isEmpty());
            
            // Add to a group
            assertTrue(getPermissionService().createGroup(TEST_GROUP1));
            getPermissionService().addMember(TEST_GROUP1, TEST_PLAYER_UUID, TEST_PLAYER, null);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm membership
            List<String> groups = Utils.toGroupNames(getPermissionService().getGroups(TEST_PLAYER_UUID));
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP1, groups.get(0));
            
            List<String> players = Utils.toMembers(getPermissionService().getMembers(TEST_GROUP1), false);
            assertEquals(1, players.size());
            assertTrue(players.contains(TEST_PLAYER));

            // Delete player
            assertTrue(getPermissionService().deleteEntity(TEST_PLAYER, TEST_PLAYER_UUID, false));
            commit();
        }
        finally {
            end();
        }
        
        begin();
        try {
            // Confirm membership
            List<String> groups = Utils.toGroupNames(getPermissionService().getGroups(TEST_PLAYER_UUID));
            assertTrue(groups.isEmpty());
            
            List<String> players = Utils.toMembers(getPermissionService().getMembers(TEST_GROUP1), false);
            assertTrue(players.isEmpty());

            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP1, null, true));
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testInheritance() {
        begin();
        try {
            // Confirm test groups are not present
            assertNull(getPermissionService().getEntity(TEST_GROUP1, null, true));
            assertNull(getPermissionService().getEntity(TEST_GROUP2, null, true));
            
            // Set up inheritance
            assertTrue(getPermissionService().createGroup(TEST_GROUP1));
            assertTrue(getPermissionService().createGroup(TEST_GROUP2));
            getPermissionService().setParent(TEST_GROUP1, TEST_GROUP2);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm
            PermissionEntity group = getPermissionService().getEntity(TEST_GROUP1, null, true);
            assertNotNull(group);
            assertFalse(group.getParents().isEmpty());
            assertEquals(TEST_GROUP2, group.getParents().get(0).getDisplayName());
            
            group = getPermissionService().getEntity(TEST_GROUP2, null, true);
            assertEquals(1, group.getInheritancesAsParent().size());

            // Attempt to set cycle
            boolean good = false;
            try {
                getPermissionService().setParent(TEST_GROUP2, TEST_GROUP1);
            }
            catch (PermissionServiceException e) {
                good = true;
            }
            assertTrue(good);
        }
        finally {
            end();
        }
        
        begin();
        try {
            // Set parent to null
            getPermissionService().setParent(TEST_GROUP1, null);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm
            PermissionEntity group = getPermissionService().getEntity(TEST_GROUP1, null, true);
            assertNotNull(group);
            assertTrue(group.getParents().isEmpty());

            group = getPermissionService().getEntity(TEST_GROUP2, null, true);
            assertTrue(group.getInheritancesAsParent().isEmpty());

            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP2, null, true));
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP1, null, true));
            commit();
        }
        finally {
            end();
        }
    }

    // Set up inheritance, delete child
    @Test
    public void testInheritanceDelete() {
        begin();
        try {
            // Confirm test groups are not present
            assertNull(getPermissionService().getEntity(TEST_GROUP1, null, true));
            assertNull(getPermissionService().getEntity(TEST_GROUP2, null, true));
            
            // Set up inheritance
            assertTrue(getPermissionService().createGroup(TEST_GROUP1));
            assertTrue(getPermissionService().createGroup(TEST_GROUP2));
            getPermissionService().setParent(TEST_GROUP1, TEST_GROUP2);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm
            PermissionEntity group = getPermissionService().getEntity(TEST_GROUP1, null, true);
            assertNotNull(group);
            assertFalse(group.getParents().isEmpty());
            assertEquals(TEST_GROUP2, group.getParents().get(0).getDisplayName());
            
            group = getPermissionService().getEntity(TEST_GROUP2, null, true);
            assertEquals(1, group.getInheritancesAsParent().size());
            
            // Purge child
            getPermissionService().deleteEntity(TEST_GROUP1, null, true);
        }
        finally {
            end();
        }

        begin();
        try {
            // Confirm
            PermissionEntity group = getPermissionService().getEntity(TEST_GROUP1, null, true);
            assertNull(group);

            group = getPermissionService().getEntity(TEST_GROUP2, null, true);
            assertTrue(group.getInheritancesAsParent().isEmpty());

            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP2, null, true));
            commit();
        }
        finally {
            end();
        }
    }
    
    // Set up inheritance, delete parent
    @Test
    public void testInheritanceDelete2() {
        begin();
        try {
            // Confirm test groups are not present
            assertNull(getPermissionService().getEntity(TEST_GROUP1, null, true));
            assertNull(getPermissionService().getEntity(TEST_GROUP2, null, true));
            
            // Set up inheritance
            assertTrue(getPermissionService().createGroup(TEST_GROUP1));
            assertTrue(getPermissionService().createGroup(TEST_GROUP2));
            getPermissionService().setParent(TEST_GROUP1, TEST_GROUP2);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm
            PermissionEntity group = getPermissionService().getEntity(TEST_GROUP1, null, true);
            assertNotNull(group);
            assertFalse(group.getParents().isEmpty());
            assertEquals(TEST_GROUP2, group.getParents().get(0).getDisplayName());
            
            group = getPermissionService().getEntity(TEST_GROUP2, null, true);
            assertEquals(1, group.getInheritancesAsParent().size());
            
            // Purge parent
            getPermissionService().deleteEntity(TEST_GROUP2, null, true);
        }
        finally {
            end();
        }

        begin();
        try {
            // Confirm
            PermissionEntity group = getPermissionService().getEntity(TEST_GROUP1, null, true);
            assertNotNull(group);
            assertTrue(group.getParents().isEmpty());

            group = getPermissionService().getEntity(TEST_GROUP2, null, true);
            assertNull(group);

            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP1, null, true));
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testGetEntities() {
        begin();
        try {
            // Confirm no entities are present
            assertNull(getPermissionService().getEntity(TEST_PLAYER, TEST_PLAYER_UUID, false));
            assertNull(getPermissionService().getEntity(TEST_GROUP1, null, true));
            assertNull(getPermissionService().getEntity(TEST_GROUP2, null, true));
            
            // Confirm lists are empty
            assertTrue(getPermissionService().getEntities(false).isEmpty());
            assertTrue(getPermissionService().getEntities(true).isEmpty());
            assertTrue(getPermissionService().getEntityNames(false).isEmpty());
            assertTrue(getPermissionService().getEntityNames(true).isEmpty());
            
            // Add a player
            getPermissionService().setPermission(TEST_PLAYER, TEST_PLAYER_UUID, false, null, null, TEST_PERMISSION, true);
            commit();
        }
        finally {
            end();
        }
        
        // Confirm
        begin();
        try {
            List<PermissionEntity> entities = getPermissionService().getEntities(false);
            assertEquals(1, entities.size());
            assertEquals(TEST_PLAYER, entities.get(0).getDisplayName());
            assertFalse(entities.get(0).isGroup());

            List<String> names = getPermissionService().getEntityNames(false);
            assertEquals(1, names.size());
            assertEquals(TEST_PLAYER, names.get(0));

            // Confirm groups still empty
            // Add a group
            getPermissionService().createGroup(TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }
        
        // Confirm group
        begin();
        try {
            List<PermissionEntity> entities = getPermissionService().getEntities(true);
            assertEquals(1, entities.size());
            assertEquals(TEST_GROUP1, entities.get(0).getDisplayName());
            assertTrue(entities.get(0).isGroup());

            List<String> names = getPermissionService().getEntityNames(true);
            assertEquals(1, names.size());
            assertEquals(TEST_GROUP1, names.get(0));

            // Add another group
            getPermissionService().createGroup(TEST_GROUP2);
            commit();
        }
        finally {
            end();
        }

        // Confirm groups
        begin();
        try {
            List<PermissionEntity> entities = getPermissionService().getEntities(true);
            assertEquals(2, entities.size());
            assertNotNull(findEntity(entities, TEST_GROUP1, true));
            assertNotNull(findEntity(entities, TEST_GROUP2, true));

            List<String> names = getPermissionService().getEntityNames(true);
            assertEquals(2, names.size());
            assertTrue(names.contains(TEST_GROUP1));
            assertTrue(names.contains(TEST_GROUP2));

            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_PLAYER, TEST_PLAYER_UUID, false));
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP2, null, true));
            assertTrue(getPermissionService().deleteEntity(TEST_GROUP1, null, true));
            commit();
        }
        finally {
            end();
        }
    }

    @Test
    public void testMetadataGetSetUnset() {
        begin();
        try {
            // Null get
            assertNull(getPermissionService().getMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA));
    
            // Missing unset
            assertFalse(getPermissionService().unsetMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA));
    
            // Set something
            getPermissionService().setMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA, TEST_STRING_VALUE);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Verify it's there
            assertEquals(TEST_STRING_VALUE, getPermissionService().getMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA));
    
            // Set to something else
            getPermissionService().setMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA, TEST_INT_VALUE);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Verify it changed
            assertEquals(TEST_INT_VALUE, getPermissionService().getMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA));
    
            // Set to something else
            getPermissionService().setMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA, TEST_REAL_VALUE);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Verify it changed
            assertEquals(TEST_REAL_VALUE, getPermissionService().getMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA));
    
            // Set to something else
            getPermissionService().setMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA, Boolean.FALSE);
            commit();
        }
        finally {
            end();
        }

        begin();
        try {
            // Verify it changed
            assertEquals(Boolean.FALSE, getPermissionService().getMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA));
    
            // Unset
            assertTrue(getPermissionService().unsetMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA));
            commit();
        }
        finally {
            end();
        }
            
        begin();
        try {
            // Verify it's gone
            assertNull(getPermissionService().getMetadata(TEST_PLAYER, TEST_PLAYER_UUID, false, TEST_METADATA));
            
            // Clean up
            assertTrue(getPermissionService().deleteEntity(TEST_PLAYER, TEST_PLAYER_UUID, false));
            commit();
        }
        finally {
            end();
        }
    }

    private PermissionEntity findEntity(List<PermissionEntity> entities, String name, boolean group) {
        for (PermissionEntity entity : entities) {
            if (entity.isGroup() == group && entity.getName().equals(name.toLowerCase()))
                return entity;
        }
        return null;
    }

}
