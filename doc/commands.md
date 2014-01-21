## Detailed Command Usage ##

### General Commands ###

*   `/permissions list <what>` &mdash; `what` should be "groups" or "players". Lists groups or players in the system. Note that only players with permissions set will be shown. (Players who are only members will not.)
*   `/permissions check <permission> [player]` &mdash; Checks if yourself or another player has the given permission. `permission` must be an unqualified permission node &mdash; no world or region qualifiers.
*   `/permissions inspect [-v] [-f <filter>] [player]` &mdash; Dumps the effective permissions of yourself or another player. Asterisked entries originate outside of zPermissions (e.g. defaults or another plugin). Use -v to also display the source of each permission (-v is default when issued from the console).
*   `/permissions diff [-r <regions>] [-R <other-regions>] [-f <filter>] <qualified-player> [other-qualified-player]` &mdash; Compare's a player's effective permissions with either another player or Bukkit's notions of effective permissions. This is a more generalized version of the `player ... diff` command as it allows specifying a different world and region(s) for each player.
*   `/permissions search [-p] [-g] [-e] [-w <world>] [-r <regions>] <permission>` &mdash; Searches for players (-p) or groups (-g) (default both) with the specified permission. Use -e along with -w and -r to search effective permissions. Note, like the `filter` parameter to other commands, the `permission` argument is matched as a substring.
*   `/permissions reload` &mdash; Re-reads config.yml.
*   `/permissions refresh [-c]` &mdash; Re-read permissions from storage and update all online players. Needed to recognize any outside changes to the zPermissions database tables. If given the -c option, the refresh will be performed conditionally, meaning the DATA_VERSION table will be checked and the refresh performed only if the version is different.
*   `/permissions export <filename>` &mdash; Creates a file containing all the zPermissions commands necessary to re-create your database. See config.yml for the output directory.
*   `/permissions import <filename>` &mdash; Executes a file containing zPermissions commands. Only works on an empty database!
*   `/permissions mygroups [-v]` &mdash; Displays a list of groups that you are a member of. Use -v to list expired memberships as well.

### Player Commands ###

*   `/permissions player <player> get <permission>` &mdash; View a permission associated with a player.
*   `/permissions player <player> set <permission> [value]` &mdash; Set a permission for a player. `value` may be "true", "t", "false", "f" or omitted.  (Defaults to true.)
*   `/permissions player <player> unset <permission>` &mdash; Remove a permission from a player.
*   `/permissions player <player> settemp [-t <timeout>] <permission> [value]` &mdash; Set a temporary permission for `timeout` seconds. See config.yml for default timeout. `permission` must be an unqualified permission node &mdash; no world or region qualifiers.
*   `/permissions player <player> purge` &mdash; Delete a player from zPermissions. Removes any permissions and group memberships.
*   `/permissions player <player> groups` &mdash; List the groups a player is a member of.
*   `/permissions player <player> setgroup <group>` &mdash; Removes all of a player's group memberships and adds them to given group.
*   `/permissions player <player> addgroup <group>` &mdash; Add player as a member of a group.
*   `/permissions player <player> removegroup <group>` &mdash; Remove player from a group.
*   `/permissions player <player> show` &mdash; Show any permissions associated with a player.
*   `/permissions player <player> dump [-w <world>] [-f <filter>] [region...]` &mdash; Evaluates permissions for the given player as if they were in the given world and region(s) and recursively dumps all permissions. Note that this will only contain permissions directly or indirectly set by zPermissions. It will not include default permissions or permissions set by other plugins. (Use `/permissions check` to check for effective permissions.) Specify a filter to only display permissions with the given substring.
*   `/permissions player <player> diff [-w <world>] [-f <filter>] <other> [region...] ` &mdash; Compares effective permissions (as given by zPermissions) with that of another player. *   `/permissions player <player> clone <new-player>` &mdash; Creates a copy of `player`, naming the copy `new-player`. Permissions and memberships are copied over.
*   `/permissions player <player> rename <new-player>` &mdash; Rename `player` as `new-player`.
*   `/permissions player <player> has <permission>` &mdash; Simply calls Bukkit's hasPermission() function for the given player and permission and outputs the result (true/false). `permission` must be an unqualified permission &mdash; no world or region qualifiers.

### Group Commands ###

*   `/permissions group <group> create` &mdash; Create a group. Note that for most commands that manipulate a group, the group must already exist!
*   `/permissions group <group> get <permission>` &mdash; View a permission associated with a group.
*   `/permissions group <group> set <permission> [value]` &mdash; Set a permission for a group. `value` may be "true", "t", "false", "f" or omitted. (Defaults to true.)
*   `/permissions group <group> unset <permission>` &mdash; Remove a permission from a group.
*   `/permissions group <group> purge` &mdash; Delete a group from zPermissions. Removes any permissions and group memberships. If the group is a parent, its child groups are orphaned.
*   `/permissions group <group> members` &mdash; List the members of the group.
*   `/permissions group <group> setparents [parent...]` &mdash; Set a group's parent group(s). If no `parent` is specified, the group will have no parent. When resolving permissions, the groups are applied in the reverse of the order given, meaning the first parent will override all subsequent parents.
*   `/permissions group <group> setweight <weight>` &mdash; Set a group's weight.
*   `/permissions group <group> add <player>` &mdash; Add a player as a member.
*   `/permissions group <group> remove <player>` &mdash; Remove a player as a member.
*   `/permissions group <group> show` &mdash; Show any permissions associated with a group.
*   `/permissions group <group> dump [-w <world>] [-f <filter>] [region...]` &mdash; Evaluates permissions for the given group as if a member were in the given world and region(s) and recursively dumps all permissions. Note that this will only contain permissions directly or indirectly set by zPermissions. It will not include default permissions or permissions set by other plugins. Specify a filter to only display permissions with the given substring.
*   `/permissions group <group> diff [-w <world>] [-f <filter>] <other> [region...]` &mdash; Compares a group's effective permissions (as given by zPermissions) with that of another.
*   `/permissions group <group> clone <new-group>` &mdash; Creates a *shallow* copy of `group`, naming the new copy `new-group`. Permissions, weight, and parents are copied. Since this is a shallow copy only, child groups of `group` are not copied.
*   `/permissions group <group> rename <new-group>` &mdash; Rename `group` as `new-group`.

### Rank Commands ###

*   `/promote <player> [track]` &mdash; Promote the player along the given track. If `track` is omitted, the default track (see config.yml) is used. If the player is not currently in any of the track's groups, they are added to the first group in the track.
*   `/demote <player> [track]` &mdash; Demote the player along the given track. If `track` is omitted, the default track is used. If the player is in the lowest group (the first group), they are removed from the group (and the track) altogether.
*   `/setrank <player> <rank> [track]` &mdash; Set the player's rank on the given track. If `track` is omitted, the default track is used.
*   `/unsetrank <player> [track]` &mdash; Remove the player from the given track. If `track` is omitted, the default track is used.

### Hidden Commands ###

Commands you probably shouldn't run often.

*   `/permissions purge [<code>]` &mdash; Deletes all players and groups. You will need to issue the command twice: once without an argument and again with the code that it gives you. Typically, the only practical use for this command is to clear permissions storage before performing a `/permissions import`
*   `/permissions cleanup` &mdash; Deletes all expired memberships. Expired memberships are basically "invisible" as far as permissions resolution is concerned. However, they will still exist in permissions storage and can still be seen (although "greyed out") using the group..members and player..groups commands. This deletes expired memberships if you really must get rid of them. Since it iterates over every single group membership, expect the command to hang your server momentarily if you have many large groups.

### Aliases ###

Convenient aliases for setting Vault-related metadata properties. See [Vault Support](http://dev.bukkit.org/server-mods/zpermissions/pages/vault-support) for more details.

*   `/permissions player <player> prefix [<prefix>]` &mdash; Set a player's chat prefix. Omit the prefix to unset.
*   `/permissions player <player> suffix [<suffix>]` &mdash; Set a player's chat suffix. Omit the suffix to unset.
*   `/permissions player <player> settrack [<track>]` &mdash; Use the given track to determine a player's "primary group." Omit the track to unset.
*   `/permissions group <group> prefix [<prefix>]` &mdash; Set a group's chat prefix. Omit the prefix to unset.
*   `/permissions group <group> suffix [<suffix>]` &mdash; Set a group's chat suffix. Omit the suffix to unset.
