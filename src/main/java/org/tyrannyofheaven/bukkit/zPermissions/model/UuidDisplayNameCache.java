/*
 * Copyright 2014 ZerothAngel <zerothangel@tyrannyofheaven.org>
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
package org.tyrannyofheaven.bukkit.zPermissions.model;

import static org.tyrannyofheaven.bukkit.util.uuid.UuidUtils.uncanonicalizeUuid;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.tyrannyofheaven.bukkit.util.uuid.UuidUtils;

@Entity
@Table(name="uuidcache")
public class UuidDisplayNameCache {

    private String name;
    
    private String displayName;

    private String uuidString;
    
    private Date timestamp;

    @Id
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(nullable=false)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Column(name="uuid", nullable=false)
    public String getUuidString() {
        return uuidString;
    }

    public void setUuidString(String uuid) {
        this.uuidString = uuid;
    }

    @Column(nullable=false)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Transient
    public UUID getUuid() {
        return uncanonicalizeUuid(getUuidString());
    }

    public void setUuid(UUID uuid) {
        setUuidString(UuidUtils.canonicalizeUuid(uuid));
    }

}
