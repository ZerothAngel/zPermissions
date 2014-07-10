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

import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionRegion;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionWorld;

public class MemoryDaoTest extends AbstractDaoTest {

    public MemoryDaoTest() {
        getPermissionService().setPermissionDao(new FilePermissionDao(getPermissionService()));
    }

    @Override
    protected void begin() {
    }
    
    @Override
    protected void commit() {
    }
    
    @Override
    protected void end() {
    }

    @Override
    protected PermissionWorld getWorld(String name) {
        return getPermissionService().getWorld(name);
    }

    @Override
    protected PermissionRegion getRegion(String name) {
        return getPermissionService().getRegion(name);
    }

}
