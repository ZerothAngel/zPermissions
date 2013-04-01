# zPermissions &mdash; A Superperms plugin for Bukkit #

zPermissions is primarily an SQL database-backed Superperms (aka Bukkit permissions) implementation. It also supports flat-file storage. Notable features are: multi-world support, ranks with multiple tracks/ladders, group inheritance of unlimited depth (within reason), and optional region-specific permissions using [WorldGuard](http://dev.bukkit.org/server-mods/worldguard/) regions.

There is no build protection (I rely on WorldGuard for that) or chat prefix support. zPermissions focuses on permissions and only permissions.

I aim to keep zPermissions a simple, yet feature-rich, Superperms provider.

Please post bugs and/or feature requests as [dev.bukkit.org tickets](http://dev.bukkit.org/server-mods/zpermissions/tickets/).

[ [Quick start documentation](http://dev.bukkit.org/server-mods/zpermissions/pages/quick-start/) | [For Server Admins](http://dev.bukkit.org/server-mods/zpermissions/pages/for-server-admins/) | [For Plugin Developers](http://dev.bukkit.org/server-mods/zpermissions/pages/for-plugin-developers/) | [Customizing Table Names](http://dev.bukkit.org/server-mods/zpermissions/pages/customizing-table-names/) ]

## Features ##

*   Uses Bukkit database to store permissions (i.e. settings in bukkit.yml). Should work with most databases supported by [Avaje Ebean](http://www.avaje.org) &mdash; I've specifically tested with PostgreSQL, MySQL, and H2. The default Bukkit database, SQLite, is **not** supported. zPermissions will automatically fall back to flat-file storage if it is used.

*   Group inheritance. Groups may inherit permissions from a single parent group.

*   Players may be members of more than one group. The order of which group permissions are applied is well defined and based on group priority (which is configurable, of course).

*   Multi-world support. Permissions granted to players and groups may be associated with a specific world.

*   Optional region support. Permissions may also be associated with WorldGuard regions.

*   Ranks! You may define multiple tracks on which to promote/demote users. Using permissions, you can also limit who can promote/demote others and which tracks they may use.

*   Short-term temporary permissions. Give a player a permission node that lasts anywhere from a few seconds to a few minutes.

*   Temporary group assignments. Assign a group to a player and have their membership expire after 1 day... a few months... or a year! Whatever duration you want.

*   With the advent of Superperms/Bukkit permissions, the recommended way of testing group membership is by using permissions. zPermissions can automatically set a permission based on the group's name for each group. By default, this configurable permission is `group.<groupname>` (compatible out-of-the-box with WorldEdit and WorldGuard!).

*   The default group (the group assigned to players who have not been explicitly placed into any groups) is named `default`. This may be changed.

## Concepts ##

*   Groups are "universal" &mdash; across all worlds. There are no plans to introduce world-specific groups.

*   However, players and groups may have world-specific and/or region-specific permissions. These permissions are only in effect when the player is in that particular world and/or region.

*   The most general permissions are applied first. So that means: universal group permissions, world-specific group permissions, region-specific universal permissions, then finally region-specific and world-specific permissions. Then repeat all that, but for player permissions.

*   Players may be members of multiple groups. Groups may be assigned a priority &mdash; a higher priority means the group is applied later so it overrides earlier groups. Groups with the same priority are applied alphabetically.

## Installation & Usage ##

Put zPermissions.jar in your server's `plugins` directory. Start up your server. This will create the file `config.yml` in your server's `plugins/zPermissions` directory. You may want to edit this file to set your default group and default track. You may also want to create your tracks.

Type `/permissions` to get started. (`/perm` or `/p` may also work, if those aliases are available.)

The permission nodes in the `get`, `set`, and `unset` sub-commands may be specified as:

*   &lt;permission> &mdash; An unqualified permission node applies to all worlds and all regions.
*   &lt;world>:&lt;permission> &mdash; To make a permission world-specific, prefix it with the world name followed by a colon.
*   &lt;region>/&lt;world>:&lt;permission> &mdash; Region-specific permissions take the above and prefix it with the region name followed by a forward-slash. For now, you may also omit the world name (so it applies to the named region in all worlds), though I'm not sure how useful this would be.

The rank commands are `/promote`, `/demote`, `/setrank`, and `/unsetrank` and will normally broadcast changes to all admins. The rank commands have an option `-q` to operate silently, e.g. when being called by automated processes. They will, however, still log their actions to the server log for security audit purposes. Opposite of `-q`, they will also take `-Q` which causes the rank commands to broadcast changes to all users.

## Permissions ##

*   zpermissions.player &mdash; `/permissions player` commands
*   zpermissions.group &mdash; `/permissions group` commands
*   zpermissions.list &mdash; `/permissions list` command
*   zpermissions.check &mdash; `/permissions check` command
*   zpermissions.check.other &mdash; `/permissions check` on other players
*   zpermissions.inspect &mdash; `/permissions inspect` command
*   zpermissions.inspect.other &mdash; `/permissions inspect` on other players
*   zpermissions.mygroups &mdash; `/permissions mygroups` command
*   zpermissions.reload &mdash; `/permissions reload` command
*   zpermissions.refresh &mdash; `/permissions refresh` command
*   zpermissions.export &mdash; `/permissions export` command
*   zpermissions.import &mdash; `/permissions import` command

### Rank Permissions ###

*   zpermissions.promote &mdash; `/promote` command
*   zpermissions.promote.* &mdash; Allows `/promote` on all tracks
*   zpermissions.promote.&lt;track> &mdash; Allows `/promote` on a specific track
*   zpermissions.demote &mdash; `/demote` command
*   zpermissions.demote.* &mdash;  Allows `/demote` on all tracks
*   zpermissions.demote.&lt;track> &mdash; Allows `/demote` on a specific track
*   zpermissions.setrank &mdash; `/setrank` command
*   zpermissions.setrank.* &mdash; Allows `/setrank` on all tracks
*   zpermissions.setrank.&lt;track> &mdash; Allows `/setrank` on a specific track
*   zpermissions.unsetrank &mdash; `/unsetrank` command
*   zpermissions.unsetrank.* &mdash; Allows `/unsetrank` on all tracks
*   zpermissions.unsetrank.&lt;track> &mdash; Allows `/unsetrank` on a specific track

### Meta-Permissions ###

*   zpermissions.* &mdash; All-inclusive permission. Given to ops by default.
*   zpermissions.rank &mdash; Use of all rank commands
*   zpermissions.rank.* &mdash; Allows rank commands on all tracks
*   zpermissions.rank.&lt;track> &mdash; Allows rank commands on a specific track

### Notification Permissions ###
*   zpermissions.notify.* &mdash; Receives all notifications.
*   zpermissions.notify.rank &mdash; Receives all rank command notifications.
*   zpermissions.notify.promote &mdash; Receives all `/promote` notifications.
*   zpermissions.notify.demote &mdash; Receives all `/demote` notifications.
*   zpermissions.notify.setrank &mdash; Receives all `/setrank` notifications.
*   zpermissions.notify.unsetrank &mdash; Receives all `/unsetrank` notifications.

For zpermissions.notify.rank (and related permissions) to work, the config.yml option rank-admin-broadcast must be set to false.

## Detailed Command Usage ##

### General Commands ###

*   `/permissions list <what>` &mdash; `what` should be "groups" or "players". Lists groups or players in the system. Note that only players with permissions set will be shown. (Players who are only members will not.)
*   `/permissions check <permission> [player]` &mdash; Checks if yourself or another player has the given permission. `permission` must be an unqualified permission node &mdash; no world or region qualifiers.
*   `/permissions inspect [-v] [-f <filter>] [player]` &mdash; Dumps the effective permissions of yourself or another player. Asterisked entries originate outside of zPermissions (e.g. defaults or another plugin). Use -v to also display the source of each permission (-v is default when issued from the console).
*   `/permissions reload` &mdash; Re-reads config.yml.
*   `/permissions refresh` &mdash; Re-read permissions from storage and update all online players. Needed to recognize any outside changes to the zPermissions database tables.
*   `/permissions export <filename>` &mdash; Creates a file containing all the zPermissions commands necessary to re-create your database. See config.yml for the output directory.
*   `/permissions import <filename>` &mdash; Executes a file containing zPermissions commands. Only works on an empty database!
*   `/permissions mygroups` &mdash; Displays a list of groups that you are a member of.

### Player Commands ###

*   `/permissions player <player> get <permission>` &mdash; View a permission associated with a player.
*   `/permissions player <player> set <permission> [value]` &mdash; Set a permission for a player. `value` may be "true", "t", "false", "f" or omitted.  (Defaults to true.)
*   `/permissions player <player> unset <permission>` &mdash; Remove a permission from a player.
*   `/permissions player <player> settemp [-t <timeout>] <permission> [value]` &mdash; Set a temporary permission for `timeout` seconds. See config.yml for default timeout. `permission` must be an unqualified permission node &mdash; no world or region qualifiers.
*   `/permissions player <player> purge` &mdash; Delete a player from zPermissions. Removes any permissions and group memberships.
*   `/permissions player <player> groups` &mdash; List the groups a player is a member of.
*   `/permissions player <player> setgroup <group>` &mdash; Removes all of a player's group memberships and adds them to given group.
*   `/permissions player <player> show` &mdash; Show any permissions associated with a player.
*   `/permissions player <player> dump [-w <world>] [-f <filter>] [region...]` &mdash; Evaluates permissions for the given player as if they were in the given world and region(s) and recursively dumps all permissions. Note that this will only contain permissions directly or indirectly set by zPermissions. It will not include default permissions or permissions set by other plugins. (Use `/permissions check` to check for effective permissions.) Specify a filter to only display permissions with the given substring.
*   `/permissions player <player> has <permission>` &mdash; Simply calls Bukkit's hasPermission() function for the given player and permission and outputs the result (true/false). `permission` must be an unqualified permission &mdash; no world or region qualifiers.

### Group Commands ###

*   `/permissions group <group> create` &mdash; Create a group. Note that for most commands that manipulate a group, the group must already exist!
*   `/permissions group <group> get <permission>` &mdash; View a permission associated with a group.
*   `/permissions group <group> set <permission> [value]` &mdash; Set a permission for a group. `value` may be "true", "t", "false", "f" or omitted. (Defaults to true.)
*   `/permissions group <group> unset <permission>` &mdash; Remove a permission from a group.
*   `/permissions group <group> purge` &mdash; Delete a group from zPermissions. Removes any permissions and group memberships. If the group is a parent, its child groups are orphaned.
*   `/permissions group <group> members` &mdash; List the members of the group.
*   `/permissions group <group> setparent [parent]` &mdash; Set a group's parent group. If `parent` is omitted, the group will have no parent.
*   `/permissions group <group> setpriority <priority>` &mdash; Set a group's priority.
*   `/permissions group <group> add <player>` &mdash; Add a player as a member.
*   `/permissions group <group> remove <player>` &mdash; Remove a player as a member.
*   `/permissions group <group> show` &mdash; Show any permissions associated with a group.
*   `/permissions player <group> dump [-w <world>] [-f <filter>] [region...]` &mdash; Evaluates permissions for the given group as if a member were in the given world and region(s) and recursively dumps all permissions. Note that this will only contain permissions directly or indirectly set by zPermissions. It will not include default permissions or permissions set by other plugins. Specify a filter to only display permissions with the given substring.

### Rank Commands ###

*   `/promote <player> [track]` &mdash; Promote the player along the given track. If `track` is omitted, the default track (see config.yml) is used. If the player is not currently in any of the track's groups, they are added to the first group in the track.
*   `/demote <player> [track]` &mdash; Demote the player along the given track. If `track` is omitted, the default track is used. If the player is in the lowest group (the first group), they are removed from the group (and the track) altogether.
*   `/setrank <player> <rank> [track]` &mdash; Set the player's rank on the given track. If `track` is omitted, the default track is used.
*   `/unsetrank <player> [track]` &mdash; Remove the player from the given track. If `track` is omitted, the default track is used.

## License & Source ##

zPermissions is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Sources may be found on GitHub:

*   [zPermissions](https://github.com/ZerothAngel/zPermissions)
*   [ToHPluginUtils](https://github.com/ZerothAngel/ToHPluginUtils)

Development builds may be found on my continous integration site:

*   [zPermissions](http://ci.tyrannyofheaven.org/job/zPermissions/) (Requires ToHPluginUtils.jar)
*   [zPermissions-standlone](http://ci.tyrannyofheaven.org/job/zPermissions-standalone/) (includes ToHPluginUtils, like the version distributed on dev.bukkit.org)

## To Do ##

*   Commands to clone/copy the permissions for a user or group.
*   More extensive unit tests, especially on the service interface.
