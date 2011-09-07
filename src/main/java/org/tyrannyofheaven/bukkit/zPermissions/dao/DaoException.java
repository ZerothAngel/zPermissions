package org.tyrannyofheaven.bukkit.zPermissions.dao;

/**
 * Thrown for any errors originating within the DAO.
 * 
 * @author zerothangel
 */
public class DaoException extends RuntimeException {

    private static final long serialVersionUID = -1915279359403745640L;

    public DaoException() {
    }

    public DaoException(String message) {
        super(message);
    }

    public DaoException(Throwable cause) {
        super(cause);
    }

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }

}
