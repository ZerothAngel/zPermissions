package org.tyrannyofheaven.bukkit.zPermissions.uuid;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.tyrannyofheaven.bukkit.zPermissions.uuid.UuidUtils.longUuidToShort;
import static org.tyrannyofheaven.bukkit.zPermissions.uuid.UuidUtils.parseUuidDisplayName;
import static org.tyrannyofheaven.bukkit.zPermissions.uuid.UuidUtils.shortUuidToLong;

import java.util.UUID;

import org.junit.Test;

public class UuidTest {

    @Test
    public void testConversion() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid.toString(), shortUuidToLong(longUuidToShort(uuid.toString())));
    }

    @Test
    public void testNoMatch() {
        UuidDisplayName udn = parseUuidDisplayName("ZerothAngel");
        assertNull(udn);
    }

    @Test
    public void testLongUuidMatchNoName() {
        UUID uuid = UUID.randomUUID();
        UuidDisplayName udn = parseUuidDisplayName(uuid.toString());
        assertNotNull(udn);
        assertEquals(uuid, udn.getUuid());
        assertNull(udn.getDisplayName());
    }

    @Test
    public void testShortUuidMatchNoName() {
        UUID uuid = UUID.randomUUID();
        UuidDisplayName udn = parseUuidDisplayName(longUuidToShort(uuid.toString()));
        assertNotNull(udn);
        assertEquals(uuid, udn.getUuid());
        assertNull(udn.getDisplayName());
    }

    @Test
    public void testLongUuidMatch() {
        UUID uuid = UUID.randomUUID();
        UuidDisplayName udn = parseUuidDisplayName(uuid.toString() + "/ZerothAngel");
        assertNotNull(udn);
        assertEquals(uuid, udn.getUuid());
        assertEquals("ZerothAngel", udn.getDisplayName());
    }

    @Test
    public void testShortUuidMatch() {
        UUID uuid = UUID.randomUUID();
        UuidDisplayName udn = parseUuidDisplayName(longUuidToShort(uuid.toString()) + "/ZerothAngel");
        assertNotNull(udn);
        assertEquals(uuid, udn.getUuid());
        assertEquals("ZerothAngel", udn.getDisplayName());
    }

}
