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

import org.junit.Test;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

public abstract class AbstractDaoTest {

    private static final String TEST_PLAYER = "Player";

    private static final String TEST_GROUP1 = "Group1";

    private static final String TEST_GROUP2 = "Group2";

    private static final String TEST_PERMISSION = "foo.bar";

    private PermissionDao dao;

    protected PermissionDao getDao() {
        return dao;
    }

    protected void setDao(PermissionDao dao) {
        this.dao = dao;
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
            assertNull(getDao().getPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));
    
            // Missing unset
            assertFalse(getDao().unsetPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));
    
            // Set something
            getDao().setPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION, true);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getDao().getPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));
    
            // Set to something else
            getDao().setPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION, false);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Verify it changed
            assertEquals(Boolean.FALSE, getDao().getPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));
    
            // Unset
            assertTrue(getDao().unsetPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));
            commit();
        }
        finally {
            end();
        }
            
        begin();
        try {
            // Verify it's gone
            assertNull(getDao().getPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));
            
            // Clean up
            assertTrue(getDao().deleteEntity(TEST_PLAYER, false));
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
            assertNull(getDao().getPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Set it
            getDao().setPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION, true);
            commit();
        }
        finally {
            end();
        }
        
        begin();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getDao().getPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Peek into world table, make sure it's there
            assertNotNull(getWorld(TEST_WORLD));
            
            // Unset
            assertTrue(getDao().unsetPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION));
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Make sure it's gone
            assertNull(getDao().getPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Should be gone from world table too
            assertNull(getWorld(TEST_WORLD));

            // Clean up
            assertTrue(getDao().deleteEntity(TEST_PLAYER, false));
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
            assertNull(getDao().getPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Set it
            getDao().setPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION, true);
            commit();
        }
        finally {
            end();
        }
        
        begin();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getDao().getPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Peek into region table, make sure it's there
            assertNotNull(getRegion(TEST_REGION));
            
            // Unset
            assertTrue(getDao().unsetPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION));
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Make sure it's gone
            assertNull(getDao().getPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Should be gone from region table too
            assertNull(getRegion(TEST_REGION));

            // Clean up
            assertTrue(getDao().deleteEntity(TEST_PLAYER, false));
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
            assertTrue(getDao().getGroups(TEST_PLAYER).isEmpty());
            
            // Add to a group
            assertTrue(getDao().createGroup(TEST_GROUP1));
            getDao().addMember(TEST_GROUP1, TEST_PLAYER);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm membership
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP1, groups.get(0));
            
            List<String> players = getDao().getMembers(TEST_GROUP1);
            assertEquals(1, players.size());
            assertTrue(players.contains(TEST_PLAYER.toLowerCase()));
    
            // Add to second group
            assertTrue(getDao().createGroup(TEST_GROUP2));
            getDao().addMember(TEST_GROUP2, TEST_PLAYER);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm membership
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(2, groups.size());
            // NB: When priorities are equal, falls back to lexicographical ordering
            assertEquals(TEST_GROUP1, groups.get(0));
            assertEquals(TEST_GROUP2, groups.get(1));
            
            List<String> players = getDao().getMembers(TEST_GROUP2);
            assertEquals(1, players.size());
            assertTrue(players.contains(TEST_PLAYER.toLowerCase()));
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Give TEST_GROUP1 a higher priority
            getDao().setPriority(TEST_GROUP1, 100);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm new ordering
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(2, groups.size());
            assertEquals(TEST_GROUP2, groups.get(0));
            assertEquals(TEST_GROUP1, groups.get(1));
            
            // Remove membership from TEST_GROUP1
            assertTrue(getDao().removeMember(TEST_GROUP1, TEST_PLAYER));
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm membership
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP2, groups.get(0));
    
            assertTrue(getDao().getMembers(TEST_GROUP1).isEmpty());
    
            // Set group
            getDao().setGroup(TEST_PLAYER, TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }
            
        begin();
        try {
            // Confirm membership
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP1, groups.get(0));
    
            // Clean up
            assertTrue(getDao().deleteEntity(TEST_GROUP2, true));
            assertTrue(getDao().deleteEntity(TEST_GROUP1, true));
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
            assertNull(getDao().getEntity(TEST_GROUP1, true));
            assertNull(getDao().getEntity(TEST_GROUP2, true));
            
            // Set up inheritance
            assertTrue(getDao().createGroup(TEST_GROUP1));
            assertTrue(getDao().createGroup(TEST_GROUP2));
            getDao().setParent(TEST_GROUP1, TEST_GROUP2);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm
            PermissionEntity group = getDao().getEntity(TEST_GROUP1, true);
            assertNotNull(group);
            assertNotNull(group.getParent());
            assertEquals(TEST_GROUP2, group.getParent().getDisplayName());
            
            // Attempt to set cycle
            boolean good = false;
            try {
                getDao().setParent(TEST_GROUP2, TEST_GROUP1);
            }
            catch (DaoException e) {
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
            getDao().setParent(TEST_GROUP1, null);
            commit();
        }
        finally {
            end();
        }
    
        begin();
        try {
            // Confirm
            PermissionEntity group = getDao().getEntity(TEST_GROUP1, true);
            assertNotNull(group);
            assertNull(group.getParent());
            
            // Clean up
            assertTrue(getDao().deleteEntity(TEST_GROUP2, true));
            assertTrue(getDao().deleteEntity(TEST_GROUP1, true));
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
            assertNull(getDao().getEntity(TEST_PLAYER, false));
            assertNull(getDao().getEntity(TEST_GROUP1, true));
            assertNull(getDao().getEntity(TEST_GROUP2, true));
            
            // Confirm lists are empty
            assertTrue(getDao().getEntities(false).isEmpty());
            assertTrue(getDao().getEntities(true).isEmpty());
            assertTrue(getDao().getEntityNames(false).isEmpty());
            assertTrue(getDao().getEntityNames(true).isEmpty());
            
            // Add a player
            getDao().setPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION, true);
            commit();
        }
        finally {
            end();
        }
        
        // Confirm
        begin();
        try {
            List<PermissionEntity> entities = getDao().getEntities(false);
            assertEquals(1, entities.size());
            assertEquals(TEST_PLAYER, entities.get(0).getDisplayName());
            assertFalse(entities.get(0).isGroup());

            List<String> names = getDao().getEntityNames(false);
            assertEquals(1, names.size());
            assertEquals(TEST_PLAYER, names.get(0));

            // Confirm groups still empty
            // Add a group
            getDao().createGroup(TEST_GROUP1);
            commit();
        }
        finally {
            end();
        }
        
        // Confirm group
        begin();
        try {
            List<PermissionEntity> entities = getDao().getEntities(true);
            assertEquals(1, entities.size());
            assertEquals(TEST_GROUP1, entities.get(0).getDisplayName());
            assertTrue(entities.get(0).isGroup());

            List<String> names = getDao().getEntityNames(true);
            assertEquals(1, names.size());
            assertEquals(TEST_GROUP1, names.get(0));

            // Add another group
            getDao().createGroup(TEST_GROUP2);
            commit();
        }
        finally {
            end();
        }

        // Confirm groups
        begin();
        try {
            List<PermissionEntity> entities = getDao().getEntities(true);
            assertEquals(2, entities.size());
            assertNotNull(findEntity(entities, TEST_GROUP1, true));
            assertNotNull(findEntity(entities, TEST_GROUP2, true));

            List<String> names = getDao().getEntityNames(true);
            assertEquals(2, names.size());
            assertTrue(names.contains(TEST_GROUP1));
            assertTrue(names.contains(TEST_GROUP2));

            // Clean up
            assertTrue(getDao().deleteEntity(TEST_PLAYER, false));
            assertTrue(getDao().deleteEntity(TEST_GROUP2, true));
            assertTrue(getDao().deleteEntity(TEST_GROUP1, true));
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
