package org.tyrannyofheaven.bukkit.zPermissions.dao;

/**
 * Thrown when referencing a non-existent group.
 * 
 * @author asaddi
 */
public class MissingGroupException extends DaoException {

    private static final long serialVersionUID = 1066889464517437579L;

    private final String groupName;
    
    public MissingGroupException(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }

}
