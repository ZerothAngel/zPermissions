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

@Entity
@Table(name="entries")
@UniqueConstraint(columnNames={"name", "is_group"})
public class Owner {

    private Long id;

    private String name;

    private boolean group;

    private Set<Entry> permissions = new HashSet<Entry>();

    private Owner parent;
    
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

    @OneToMany(cascade = CascadeType.ALL)
    public Set<Entry> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Entry> permissions) {
        this.permissions = permissions;
    }

    @ManyToOne(optional=true)
    public Owner getParent() {
        return parent;
    }

    public void setParent(Owner parent) {
        this.parent = parent;
    }

}
