package org.tyrannyofheaven.bukkit.zPermissions;

import java.util.Collections;

import org.tyrannyofheaven.bukkit.zPermissions.dao.FilePermissionDao;
import org.tyrannyofheaven.bukkit.zPermissions.dao.InMemoryPermissionService;

public class MemoryResolverTest extends AbstractResolverTest {

    public MemoryResolverTest() {
        permissionService = new InMemoryPermissionService();
        permissionService.setPermissionDao(new FilePermissionDao(permissionService));
        resolver = new PermissionsResolver(permissionService);
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
