package org.tyrannyofheaven.bukkit.zPermissions;

import org.tyrannyofheaven.bukkit.util.command.Command;

public class RootCommand {

    private final SubCommands sc = new SubCommands();

    @Command("perm")
    public Object perm() {
        return sc;
    }

}
