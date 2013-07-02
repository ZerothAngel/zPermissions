# Frequently Asked Questions #

*   **Where is the '\*' node?**

    zPermissions adheres strictly to the Bukkit 'SuperPerms' API, which does not support wildcards of any kind. (No, not even `plugin.*` -type permissions are supported &mdash; these need to be implemented by the plugins themselves.) As suggested by mbaxter in his ["Why PEX is broken"](http://goo.gl/MHhbl) document, if you want something like the '*' node, give that user OP status instead. If you don't want them to be able to OP others, negate the nodes for the OP command:
    
        /permissions player <player> set bukkit.command.op.give false
        /permissions player <player> set bukkit.command.op.take false

    (Needless to say, you should also negate `zpermissions.*` or at least both `zpermissions.player` and `zpermissions.group` so they can't modify permissions that will allow them to OP others or allow others to OP.)

*   **Do you have a converter for permissions plugin X?**

    No\*, and there are no plans to add any converter *built-into* zPermissions. Maybe as a separate jar or a separate project. And I would certainly welcome it if someone else stepped up to do so. But I'm not really motivated to do it myself because:
    
    1. Permission models tend to vary from plugin to plugin. Though perhaps the closest plugin to zPermissions in terms of model would be PermissionsBukkit and any of its derivatives.
    2. I have no desire to track the file formats or other details of other plugins in zPermissions. Heck, zPerms' schema/flat file format has changed many times in the last few months. I always aim to make things backwards compatible, and I would rather expend that effort on my own project.

    \* Despite having said all that, I do have a [very basic converter](https://bitbucket.org/ZerothAngel/tozperms) available. It currently only supports PEX, and performs a lossy conversion at best.

*   **Can zPermissions co-exist with other permissions plugins?**

    Aside from potential command conflicts, yes. (But perhaps the question has other types of plugins in mind &mdash; not just strictly permissions plugins.)
    
    zPermissions never touches permissions set by other plugins. Under the hood, zPermissions creates its own PermissionAttachment and will only ever modify the one that it explicitly created.
    
    Of course, whether or not it can co-exist with plugin X also depends on whether plugin X also plays nicely...

*   **Why another permissions plugin?**

    Well, I don't actually get this question much anymore. But in short:
    
    1. None of the current plugins *at the time* met my needs. PermissionsBukkit had buggy multi-world support and the developer did not seem very responsive. bPermissions did not have group inheritance and changed its file format at least twice while I used it (without providing an auto-upgrade path). Lastly, I did not want to touch anything that still clung to the old "Permissions 2" system.
    2. I wanted region-specific permissions.
    3. The first plugin I developed ([Excursion](http://dev.bukkit.org/server-mods/excursion/)) was relatively simple. I wanted a bit more practice with the Bukkit API, so I coded zPermissions from scratch.
    4. It was the primary testbed and driver for development for my [personal plugin library](https://github.com/ZerothAngel/ToHPluginUtils).
    
*   **Can zPermissions support region plugin X?**

    It should be easier to do now, so feel free to [create a ticket](http://dev.bukkit.org/server-mods/zpermissions/tickets/). If you provide links to API docs and/or provide configuration examples, you will greatly improve the chance that I will accept it and work on it sooner rather than later. ;)
    
    There's only one hard requirement on the region plugin: it must be able to return the name(s) of regions that enclose any given location.
    
    If the region plugin is a Maven project and/or is hosted in a Maven repository somewhere, that is also a major plus.

*   **What does the `opaque-inheritance` option do?**

    When `opaque-inheritance` is true (the default setting), each assigned group is fully resolved before moving on to the next. This means *the ancestor groups* of higher-weight assigned groups take precedence over lower-weight groups.
    
    When false, ancestor groups that were already resolved are no longer resolved again.
    
    To help illustrate, suppose there are 5 groups: *A B C D E*
    
    *A*'s parent is *B*<br>
    *B*'s parent is *E*<br>
    *C*'s parent is *D*<br>
    *D*'s parent is *E*
    
    *A* has weight 100.<br>
    *C* has weight 0.
    
    A player, Bob, has both groups *A* and *C* assigned. Since *A* has higher weight, it is resolved last and therefore has highest precedence.
    
    With `opaque-inheritance` true, the first step is to fully resolve group *C*. So the resolution order is:
    
    *E D C*
    
    (The permissions of a child group are always applied after its parent.) Next, group *A* is fully resolved, so the new order becomes:
    
    *E D C E B A*
    
    You'll notice *E* appears twice. Since resolving a group twice has no real effect (e.g. if you set a permission to true twice, it's still true), this is in effect:
    
    *D C E B A*
    
    And this is the order which each group's permissions are applied.
    
    Now with `opaque-inheritance` false, the first step again is to resolve group *C*:
    
    *E D C*
    
    Next,  group *A* is fully resolved. But wait! One of *A*'s ancestors is group *E* which is already on this list. So it is skipped.
    
    *E D C B A*
    
    And that leads to the very subtle difference between both resolution strategies.
    
    Basically, if you *flatten* each group and resolve them in order, you will get the same effect as `opaque-inheritance` true &mdash; in the above example, *E* has the potential to override permissions from groups *D* and *C* because it is an ancestor of a higher-weight group (*A*). (It is considered part of *A* and not a separate group in itself.)
    
    But if you want to treat individual ancestor groups as separate groups in themselves, then `opaque-inheritance` false will give that effect &mdash; *E*, being the base group of both *A* and *C*, can have its permissions potentially overridden by any other group.

*   **What does the `interleaved-player-permissions` option do?**

    When true (the default from version 0.9 - 0.9.19), player permissions are interleaved with group permissions at the same "level" (universal, world, region, region/world):

     1. Universal group permissions
     2. Universal player permissions
     3. World-specific group permissions
     4. World-specific player permissions
     5. Region-specific group permissions
     6. Region-specific player permissions
     7. Region- and world-specific group permissions
     8. Region- and world-specific player permissions

    When false, player permissions are only applied once all group permissions have been applied:

     1. Universal group permissions
     2. World-specific group permissions
     3. Region-specific group permissions
     4. Region- and world-specific group permissions
     5. Universal player permissions
     6. World-specific player permissions
     7. Region-specific player permissions
     8. Region- and world-specific player permissions

*   **What is `auto-refresh-interval` for?**

    The `auto-refresh-interval` option, which is disabled by default (e.g. a negative number), periodically performs the equivalent of `/permissions refresh` at some given interval. Theoretically, you could set up multiple servers to share a single zPermissions database. (However, the fact that this would be configured through bukkit.yml, which is also used by other plugins, limits this somewhat.) More practically, you could have an external website or forum handle player registration and automatically promote players.

    Since zPermissions permanently caches data for performance reasons, you will need some way to tell zPermissions to re-read the database. And that's where `/permissions refresh` and `auto-refresh-interval` come in.

    If you aren't doing either of these things (multi-server or writing to the database from an external source), it is probably best to leave `auto-refresh-interval` disabled.
