package org.tyrannyofheaven.bukkit.zPermissions;

/**
 * Exception thrown when attempting a write operation when configured as read-only.
 * 
 * @author zerothangel
 */
public class ReadOnlyException extends RuntimeException {

    private static final long serialVersionUID = -1190631455357421902L;

    public ReadOnlyException() {
    }

    public ReadOnlyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReadOnlyException(String message) {
        super(message);
    }

    public ReadOnlyException(Throwable cause) {
        super(cause);
    }

}
