package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.Collections;

import org.tyrannyofheaven.bukkit.zPermissions.dao.MemoryPermissionDao;

public class MemoryResolverTest extends AbstractResolverTest {

    public MemoryResolverTest() {
        dao = new MemoryPermissionDao();
        resolver = new PermissionsResolver(dao);
        resolver.setDefaultGroup(TEST_GROUP1);
        resolver.setGroupPermissionFormats(Collections.singleton("group.%s"));
        resolver.setAssignedGroupPermissionFormats(Collections.singleton("assignedgroup.%s"));
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

}
