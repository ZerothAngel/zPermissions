## Permissions ##

### Meta-Permissions ###

*   zpermissions.* &mdash; All-inclusive permission. Given to ops by default.
*   zpermissions.player.* &mdash; All `/permissions player` commands
*   zpermissions.group.* &mdash; All `/permissions group` commands
*   zpermissions.rank &mdash; Use of all rank commands
*   zpermissions.rank.* &mdash; Allows rank commands on all tracks
*   zpermissions.rank.&lt;track> &mdash; Allows rank commands on a specific track

### General Permissions ###

*   zpermissions.player.view &mdash; `/permissions player` read-only management commands
*   zpermissions.player.manage &mdash; `/permissions player` management commands
*   zpermissions.player.chat &mdash; `/permissions player` chat commands
*   zpermissions.group.view &mdash; `/permissions group` read-only management commands
*   zpermissions.group.manage &mdash; `/permissions group` management commands
*   zpermissions.group.chat &mdash; `/permissions group` chat commands
*   zpermissions.list &mdash; `/permissions list` command
*   zpermissions.check &mdash; `/permissions check` command
*   zpermissions.check.other &mdash; `/permissions check` on other players
*   zpermissions.inspect &mdash; `/permissions inspect` command
*   zpermissions.inspect.other &mdash; `/permissions inspect` on other players
*   zpermissions.diff &mdash; `/permissions diff` command
*   zpermissions.search &mdash; `/permissions search` command
*   zpermissions.mygroups &mdash; `/permissions mygroups` command
*   zpermissions.reload &mdash; `/permissions reload` command
*   zpermissions.refresh &mdash; `/permissions refresh` command
*   zpermissions.export &mdash; `/permissions export` command
*   zpermissions.import &mdash; `/permissions import` command
*   zpermissions.purge &mdash; `/permissions purge` command

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

### Notification Permissions ###
*   zpermissions.notify.* &mdash; Receives all notifications.
*   zpermissions.notify.rank &mdash; Receives all rank command notifications.
*   zpermissions.notify.promote &mdash; Receives all `/promote` notifications.
*   zpermissions.notify.demote &mdash; Receives all `/demote` notifications.
*   zpermissions.notify.setrank &mdash; Receives all `/setrank` notifications.
*   zpermissions.notify.unsetrank &mdash; Receives all `/unsetrank` notifications.
*   zpermissions.notify.expiration &mdash; Receives temporary membership expiration notifications.

For zpermissions.notify.rank (and related permissions) to work, the config.yml option rank-admin-broadcast must be set to false.
