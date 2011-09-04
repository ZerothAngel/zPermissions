package org.tyrannyofheaven.bukkit.zPermissions.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("G")
public class Group extends Owner {

    private Group parent;

    @ManyToOne(optional=true)
    public Group getParent() {
        return parent;
    }

    public void setParent(Group parent) {
        this.parent = parent;
    }
    
}
