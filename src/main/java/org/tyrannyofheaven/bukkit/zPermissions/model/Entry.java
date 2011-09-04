package org.tyrannyofheaven.bukkit.zPermissions.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.UniqueConstraint;

@Entity
@UniqueConstraint(columnNames={"owner_id", "permission"})
public class Entry {

    private Long id;

    private Owner owner;

    private String permission;
    
    private boolean value;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne(optional=false)
    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
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

}
