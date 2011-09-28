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
package org.tyrannyofheaven.bukkit.zPermissions.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.avaje.ebean.annotation.CacheStrategy;

/**
 * The permission entity &mdash; something that can have a set of permission
 * entries. Originally I had different classes for players and groups, but
 * Avaje's handling of inheritance seems a bit wonky. So I've basically
 * collapsed both classes into this single class, using {@link #isGroup()} as
 * a discriminator. Ugh...
 * 
 * @author zerothangel
 */
@Entity
@Table(name="entities")
@UniqueConstraint(columnNames={"name", "is_group"})
@CacheStrategy(useBeanCache=true)
public class PermissionEntity {

    private Long id;

    private String name;

    private boolean group;

    private String displayName;

    private int priority;

    private PermissionEntity parent;
    
    private Set<Entry> permissions = new HashSet<Entry>();

    private Set<PermissionEntity> children = new HashSet<PermissionEntity>();
    
    private Set<Membership> memberships = new HashSet<Membership>();

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(nullable=false)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name="is_group", nullable=false)
    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    @Column(nullable=false)
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @ManyToOne(optional=true)
    public PermissionEntity getParent() {
        return parent;
    }

    public void setParent(PermissionEntity parent) {
        this.parent = parent;
    }

    @OneToMany(mappedBy="entity", cascade=CascadeType.ALL)
    public Set<Entry> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Entry> permissions) {
        this.permissions = permissions;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @OneToMany(mappedBy="parent")
    public Set<PermissionEntity> getChildren() {
        return children;
    }

    public void setChildren(Set<PermissionEntity> children) {
        this.children = children;
    }

    @OneToMany(mappedBy="group", cascade=CascadeType.ALL)
    public Set<Membership> getMemberships() {
        return memberships;
    }

    public void setMemberships(Set<Membership> memberships) {
        this.memberships = memberships;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof PermissionEntity)) return false;
        PermissionEntity o = (PermissionEntity)obj;
        return getName().equals(o.getName()) &&
            isGroup() == o.isGroup();
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + getName().hashCode();
        result = 37 * result + Boolean.valueOf(isGroup()).hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("Entity[%s,%s]", getName(), isGroup());
    }

}
