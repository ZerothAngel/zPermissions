package org.tyrannyofheaven.bukkit.zPermissions.model;


import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("P")
public class Player extends Owner {

}
