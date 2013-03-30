/*
 * Copyright 2013 ZerothAngel <zerothangel@tyrannyofheaven.org>
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

import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;

import com.avaje.ebean.config.TableName;
import com.avaje.ebean.config.UnderscoreNamingConvention;

class ZPermissionsNamingConvention extends UnderscoreNamingConvention {

    private final Map<String, String> tableNames = new HashMap<String, String>();

    ZPermissionsNamingConvention(JavaPlugin plugin) {
        // Set up null placeholders
        for (Class<?> clazz : plugin.getDatabaseClasses()) {
            tableNames.put(clazz.getSimpleName(), null);
        }
    }
    
    void clearTableNames() {
        for (Map.Entry<String, String> me : tableNames.entrySet()) {
            me.setValue(null);
        }
    }

    void setTableName(String className, String tableName) {
        if (!ToHStringUtils.hasText(tableName))
            tableName = null; // Normalize
        if (tableNames.containsKey(className)) {
            tableNames.put(className, tableName);
        }
    }

    @Override
    public TableName getTableName(Class<?> beanClass) {
        String qualifiedTableName = tableNames.get(beanClass.getSimpleName());
        if (qualifiedTableName != null) {
            return new TableName(qualifiedTableName);
        }
        return super.getTableName(beanClass);
    }

}