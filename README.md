# zPermissions &mdash; A Superperms plugin for Bukkit #

zPermissions is an SQL database-backed Superperms (aka Bukkit permissions)
implementation. Notable features are: multi-world support, ranks with multiple
tracks/ladders, unlimited group inheritance (within reason), and optional
region-specific permissions using [WorldGuard](http://dev.bukkit.org/server-mods/worldguard/) regions.

There's no Permissions 2 or 3 support (and none is planned &mdash; ever).
However, zPermissions works great with [PermissionsBukkit](http://dev.bukkit.org/server-mods/permbukkit/)'s
SuperpermsBridge.

Lastly, there is currently no build protection as I rely on WorldGuard for that.
I aim to keep zPermissions a simple, yet feature-rich, Superperms provider.

## Features ##

*   Uses Bukkit database to store permissions (i.e. settings in bukkit.yml). Note however,
    SQLite, the default database for Bukkit, does **not** work. You must use a
    real database. I know this will scare away 99% of prospective users. :P
	
    I developed with [H2](http://www.h2database.com/) and will probably deploy
	with it. [PostgreSQL](http://www.postgresql.org/) also seems to
	work fine and I'm sure [MySQL](http://dev.mysql.com/) will as well.

*   Group inheritance. Groups may inherit permissions from a single parent
	group.

*   Players may be members of more than one group. The order of which group
	permissions are applied is well defined and based on group priority
	(which is configurable, of course).

*   Multi-world support. Each group may define world-specific permissions.

*   Optional region support. Permissions may be associated with WorldGuard
    regions.

*   Ranks! You may define multiple tracks on which to promote/demote users.
	Using permissions, you can also limit who can promote/demote others and
	which tracks they may use.

*   With the advent of Superperms/Bukkit permissions, the recommended
    way of testing group membership is by using permissions. zPermissions
	can automatically set a permission based on the group's name for each
	group. By default, this configurable permission is `group.<groupname>`.

*   The default group (the group assigned to players who have not been
	explicitly placed into any groups) is named `default`. This may be changed.

## Concepts ##

*   Groups are global &mdash; across all worlds. There are no plans to introduce
    world-specific groups.

*   However, players and groups may have world-specific and/or region-specific
    permissions. These permissions are only in effect when the player is in
    that particular world and/or region.

*   The most general permissions are applied first. So that means: global group
    permissions, world-specific group permissions, region-specific global
    permissions, then finally region-specific and world-specific permissions.
    Then repeat all that, but for player permissions.

*   Players may be members of multiple groups. Groups may be assigned a
    priority &mdash; a higher priority means the group is applied later so it
	overrides earlier groups. Groups with the same priority are applied
	alphabetically.

## Installation & Usage ##

Put zPermissions.jar in your server's `plugins` directory. Start up your server.
This will create the file `config.yml` in your server's `plugins/zPermissions`
directory. You may want to edit this file to set your default group and
default track. You may also want to create your tracks.

Type `/permissions` to get started. (`/perm` or `/p` may also work, if
those aliases are available.)

The permission nodes in the `get`, `set`, and `unset` sub-commands may
be specified as:

*   &lt;permission> &mdash; An unqualified permission node applies to all
    worlds and all regions.
*   &lt;world>:&lt;permission> &mdash; To make a permission world-specific,
    prefix it with the world name followed by a colon.
*   &lt;region>/&lt;world>:&lt;permission> &mdash; Region-specific permissions
    take the above and prefix it with the region name followed by a
    forward-slash. For now, you may also omit the world name (so it applies
    to the named region in all worlds), though I'm not sure how useful
    this would be.

The rank commands are `/promote` and `/demote`.

## Permissions ##

*   zpermissions.* &mdash; All-inclusive permission. Given to ops by default.
*   zpermissions.player &mdash; `/permissions player` commands
*   zpermissions.group &mdash; `/permissions group` commands
*   zpermissions.list &mdash; `/permissions list` command
*   zpermissions.check &mdash; `/permissions check` command
*   zpermissions.check.other &mdash; `/permissions check` on other players
*   zpermissions.reload &mdash; `/permissions reload` command
*   zpermissions.export &mdash; `/permissions export` command
*   zpermissions.import &mdash; `/permissions import` command
*   zpermissions.promote &mdash; `/promote` command
*   zpermissions.promote.* &mdash; Allows `/promote` on all tracks
*   zpermissions.promote.&lt;track> &mdash; Allows `/promote` on a specific
    track
*   zpermissions.demote &mdash; `/demote` command
*   zpermissions.demote.* &mdash;  Allows `/demote` on all tracks
*   zpermissions.demote.&lt;track> &mdash; Allows `/demote` on a specific track
