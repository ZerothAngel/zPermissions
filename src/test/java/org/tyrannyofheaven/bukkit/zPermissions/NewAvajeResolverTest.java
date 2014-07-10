/*
 * Copyright 2011 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
package org.tyrannyofheaven.bukkit.zPermissions;


import java.util.Collections;

import org.tyrannyofheaven.bukkit.zPermissions.dao.AvajePermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;

public class NewAvajeResolverTest extends AbstractResolverTest {

    private static EbeanServer ebeanServer;

    public NewAvajeResolverTest() {
        // Bleh. But Avaje doesn't like being initialized more than once.
        // And Maven instantiates this class for each test.
        synchronized (NewAvajeResolverTest.class) {
            if (ebeanServer == null) {
                ServerConfig config = new ServerConfig();
                config.setName(NewAvajeResolverTest.class.getSimpleName());
                config.setDefaultServer(false);

                DataSourceConfig dataSourceConfig = new DataSourceConfig();
                dataSourceConfig.setDriver("org.h2.Driver");
                dataSourceConfig.setUsername("username");
                dataSourceConfig.setPassword("password");
                dataSourceConfig.setUrl("jdbc:h2:NewAvajeResolverTest");

                config.setDataSourceConfig(dataSourceConfig);

                config.setDdlGenerate(true);
                config.setDdlRun(true);

                config.addClass(PermissionEntity.class);
                config.addClass(Inheritance.class);
                config.addClass(PermissionRegion.class);
                config.addClass(PermissionWorld.class);
                config.addClass(Entry.class);
                config.addClass(Membership.class);
                config.addClass(EntityMetadata.class);

                ebeanServer = EbeanServerFactory.create(config);
            }
        }

        permissionService = new InMemoryPermissionService();
        permissionService.setPermissionDao(new AvajePermissionDao(permissionService, ebeanServer, null));
        resolver = new PermissionsResolver(permissionService);
        resolver.setDefaultGroup(TEST_GROUP1);
        resolver.setGroupPermissionFormats(Collections.singleton("group.%s"));
        resolver.setAssignedGroupPermissionFormats(Collections.singleton("assignedgroup.%s"));
    }

    EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    protected void begin() {
        getEbeanServer().beginTransaction();
    }
    
    protected void commit() {
        getEbeanServer().commitTransaction();
    }
    
    protected void end() {
        getEbeanServer().endTransaction();
    }

}
