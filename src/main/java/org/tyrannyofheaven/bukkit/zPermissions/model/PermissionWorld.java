package org.tyrannyofheaven.bukkit.zPermissions.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="worlds")
@UniqueConstraint(columnNames="name")
public class PermissionWorld {

    private Long id;
    
    private String name;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof PermissionWorld)) return false;
        PermissionWorld o = (PermissionWorld)obj;
        return getName().equals(o.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

}
