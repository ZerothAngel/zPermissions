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

public class NewAvajeDaoTest extends AbstractDaoTest {

    private static EbeanServer ebeanServer;

    public NewAvajeDaoTest() {
        // Bleh. But Avaje doesn't like being initialized more than once.
        // And Maven instantiates this class for each test.
        synchronized (NewAvajeDaoTest.class) {
            if (ebeanServer == null) {
                ServerConfig config = new ServerConfig();
                config.setName(NewAvajeDaoTest.class.getSimpleName());
                config.setDefaultServer(false);

                DataSourceConfig dataSourceConfig = new DataSourceConfig();
                dataSourceConfig.setDriver("org.h2.Driver");
                dataSourceConfig.setUsername("username");
                dataSourceConfig.setPassword("password");
                dataSourceConfig.setUrl("jdbc:h2:NewAvajeDaoTest");

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

        getPermissionService().setPermissionDao(new AvajePermissionDao(getPermissionService(), ebeanServer, null));
    }

    EbeanServer getEbeanServer() {
        return ebeanServer;
    }

    @Override
    protected void begin() {
        getEbeanServer().beginTransaction();
    }
    
    @Override
    protected void commit() {
        getEbeanServer().commitTransaction();
    }
    
    @Override
    protected void end() {
        getEbeanServer().endTransaction();
    }

    @Override
    protected PermissionWorld getWorld(String name) {
        return getEbeanServer().find(PermissionWorld.class).where().eq("name", name).findUnique();
    }

    @Override
    protected PermissionRegion getRegion(String name) {
        return getEbeanServer().find(PermissionRegion.class).where().eq("name", name).findUnique();
    }

}
