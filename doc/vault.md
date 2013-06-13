## Vault Support ##

zPermissions more or less implements 100% of the [Vault](http://dev.bukkit.org/server-mods/vault/) APIs for both permissions and chat. Currently, zPermissions support is built-in to Vault.

### Vault Permissions API Notes ###

*   Permissions support was added at around Vault 1.2.13.
*   All API methods are supported.
*   All player-related methods should work on offline players.
*   Due to restrictions I've placed on the zPermissions API, all Vault API methods that modify permissions or membership will execute equivalent commands as console.
*   zPermissions groups are universal &mdash; they apply to all worlds. Therefore, the world argument to playerAddGroup() and playerRemoveGroup() is ignored.
*   zPermissions has no real concept of a "primary group." By default, the primary group of a player is the highest-weight group assigned to that player, i.e. the first group returned when doing a `/permissions player ... groups` or `/permissions mygroups` command.

Starting with Vault 1.2.26, the primary group can also be controlled by using a metadata property on a player.

For example:

    /permissions player Alice metadata set Vault.primary-group.track Admin

Or alternatively:

    /permissions player Alice settrack Admin

Assuming Alice has been previously promoted on the Admin track, her primary group will be the highest-ranked group from that track regardless of assignment priorities.

If Alice is not on the Admin track then the behavior returns to the default, i.e. the highest-weight assigned group.

### Vault Chat API Notes ###

*   Chat support was added in Vault 1.2.25.
*   The primary group is determined as explained above.
*   All Vault API "player info" and "group info" methods are supported. They map directly to zPermissions metadata properties.
*   All Vault API methods that set prefix/suffix/infos will execute an equivalent command as console.
*   Prefixes and suffixes are set by the `prefix` and `suffix` (string) metadata properties on both players and groups.

Examples:

    /permissions group VIP md set prefix [VIP]
    /permissions player Bob metadata set prefix &f
    /permissions group Mod prefix "[Mod: "
    /permissions group Mod suffix ]
