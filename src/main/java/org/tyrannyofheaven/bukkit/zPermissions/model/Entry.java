package org.tyrannyofheaven.bukkit.zPermissions.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="entries")
@UniqueConstraint(columnNames={"entity_id", "permission"})
public class Entry {

    private Long id;

    private PermissionEntity entity;

    private String permission;
    
    private boolean value;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JoinColumn(name="entity_id")
    @ManyToOne(optional=false)
    public PermissionEntity getEntity() {
        return entity;
    }

    public void setEntity(PermissionEntity owner) {
        this.entity = owner;
    }

    @Column(nullable=false)
    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Column(nullable=false)
    public boolean isValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if ((obj instanceof Entry)) return false;
        Entry o = (Entry)obj;
        return getEntity().equals(o.getEntity()) &&
            getPermission().equals(o.getPermission());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + getEntity().hashCode();
        result = 37 * result + getPermission().hashCode();
        return result;
    }

}
