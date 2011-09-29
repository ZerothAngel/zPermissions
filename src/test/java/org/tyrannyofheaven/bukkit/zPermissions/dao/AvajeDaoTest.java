/*
 * Copyright 2011 Allan Saddi <allan@saddi.com>
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
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;

public class AvajeDaoTest {

    private static final String TEST_PLAYER = "Player";
    
    private static final String TEST_GROUP1 = "Group1";
    
    private static final String TEST_GROUP2 = "Group2";
    
    private static final String TEST_PERMISSION = "foo.bar";

    private static EbeanServer ebeanServer;

    private final PermissionDao dao;

    EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    PermissionDao getDao() {
        return dao;
    }

    public AvajeDaoTest() {
        // Bleh. But Avaje doesn't like being initialized more than once.
        // And Maven instantiates this class for each test.
        synchronized (AvajeDaoTest.class) {
            if (ebeanServer == null) {
                ServerConfig config = new ServerConfig();
                config.setName(AvajeDaoTest.class.getSimpleName());
                config.setDefaultServer(false);

                DataSourceConfig dataSourceConfig = new DataSourceConfig();
                dataSourceConfig.setDriver("org.h2.Driver");
                dataSourceConfig.setUsername("username");
                dataSourceConfig.setPassword("password");
                dataSourceConfig.setUrl("jdbc:h2:AvajeDaoTest");

                config.setDataSourceConfig(dataSourceConfig);

                config.setDdlGenerate(true);
                config.setDdlRun(true);

                config.addClass(PermissionEntity.class);
                config.addClass(PermissionRegion.class);
                config.addClass(PermissionWorld.class);
                config.addClass(Entry.class);
                config.addClass(Membership.class);

                ebeanServer = EbeanServerFactory.create(config);
            }
        }

        dao = new AvajePermissionDao(ebeanServer);
    }

    @Test
    public void testGetSetUnset() {
        getEbeanServer().beginTransaction();
        try {
            // Null get
            assertNull(getDao().getPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));

            // Missing unset
            assertFalse(getDao().unsetPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));

            // Set something
            getDao().setPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION, true);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getDao().getPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));

            // Set to something else
            getDao().setPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION, false);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
        try {
            // Verify it changed
            assertEquals(Boolean.FALSE, getDao().getPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));

            // Unset
            assertTrue(getDao().unsetPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
            
        getEbeanServer().beginTransaction();
        try {
            // Verify it's gone
            assertNull(getDao().getPermission(TEST_PLAYER, false, null, null, TEST_PERMISSION));
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Test
    public void testWorldPermission() {
        final String TEST_WORLD = "myworld";
        
        getEbeanServer().beginTransaction();
        try {
            // Confirm missing world-specific permission
            assertNull(getDao().getPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Set it
            getDao().setPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION, true);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
        
        getEbeanServer().beginTransaction();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getDao().getPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Peek into world table, make sure it's there
            assertNotNull(getEbeanServer().find(PermissionWorld.class).where().eq("name", TEST_WORLD));
            
            // Unset
            assertTrue(getDao().unsetPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION));
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
        try {
            // Make sure it's gone
            assertNull(getDao().getPermission(TEST_PLAYER, false, null, TEST_WORLD, TEST_PERMISSION));
            
            // Should be gone from world table too
            assertNull(getEbeanServer().find(PermissionWorld.class).where().eq("name", TEST_WORLD).findUnique());
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Test
    public void testRegionPermission() {
        // Regions are just like worlds
        final String TEST_REGION = "myregion";
        
        getEbeanServer().beginTransaction();
        try {
            // Confirm missing world-specific permission
            assertNull(getDao().getPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Set it
            getDao().setPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION, true);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
        
        getEbeanServer().beginTransaction();
        try {
            // Verify it's there
            assertEquals(Boolean.TRUE, getDao().getPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Peek into region table, make sure it's there
            assertNotNull(getEbeanServer().find(PermissionRegion.class).where().eq("name", TEST_REGION));
            
            // Unset
            assertTrue(getDao().unsetPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION));
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
        try {
            // Make sure it's gone
            assertNull(getDao().getPermission(TEST_PLAYER, false, TEST_REGION, null, TEST_PERMISSION));
            
            // Should be gone from region table too
            assertNull(getEbeanServer().find(PermissionRegion.class).where().eq("name", TEST_REGION).findUnique());
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Test
    public void testMembership() {
        getEbeanServer().beginTransaction();
        try {
            // Should not be in any groups
            assertTrue(getDao().getGroups(TEST_PLAYER).isEmpty());
            
            // Add to a group
            getDao().addMember(TEST_GROUP1, TEST_PLAYER);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
        try {
            // Confirm membership
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP1, groups.get(0));
            
            List<String> players = getDao().getMembers(TEST_GROUP1);
            assertEquals(1, players.size());
            assertTrue(players.contains(TEST_PLAYER.toLowerCase()));

            // Add to second group
            getDao().addMember(TEST_GROUP2, TEST_PLAYER);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
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

            // Give TEST_GROUP1 a higher priority
            getDao().setPriority(TEST_GROUP1, 100);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
        try {
            // Confirm new ordering
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(2, groups.size());
            assertEquals(TEST_GROUP2, groups.get(0));
            assertEquals(TEST_GROUP1, groups.get(1));
            
            // Remove membership from TEST_GROUP1
            assertTrue(getDao().removeMember(TEST_GROUP1, TEST_PLAYER));
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
        try {
            // Confirm membership
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP2, groups.get(0));

            assertTrue(getDao().getMembers(TEST_GROUP1).isEmpty());

            // Set group
            getDao().setGroup(TEST_PLAYER, TEST_GROUP1);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
            
        getEbeanServer().beginTransaction();
        try {
            // Confirm membership
            List<String> groups = getDao().getGroups(TEST_PLAYER);
            assertEquals(1, groups.size());
            assertEquals(TEST_GROUP1, groups.get(0));

            // Clean up
            assertTrue(getDao().deleteEntity(TEST_GROUP2, true));
            assertTrue(getDao().deleteEntity(TEST_GROUP1, true));
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

    @Test
    public void testInheritance() {
        getEbeanServer().beginTransaction();
        try {
            // Confirm test groups are not present
            assertNull(getDao().getEntity(TEST_GROUP1, true));
            assertNull(getDao().getEntity(TEST_GROUP2, true));
            
            // Set up inheritance
            getDao().setParent(TEST_GROUP1, TEST_GROUP2);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
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
            getEbeanServer().endTransaction();
        }
        
        getEbeanServer().beginTransaction();
        try {
            // Set parent to null
            getDao().setParent(TEST_GROUP1, null);
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }

        getEbeanServer().beginTransaction();
        try {
            // Confirm
            PermissionEntity group = getDao().getEntity(TEST_GROUP1, true);
            assertNotNull(group);
            assertNull(group.getParent());
            
            // Clean up
            assertTrue(getDao().deleteEntity(TEST_GROUP2, true));
            assertTrue(getDao().deleteEntity(TEST_GROUP1, true));
            getEbeanServer().commitTransaction();
        }
        finally {
            getEbeanServer().endTransaction();
        }
    }

}
