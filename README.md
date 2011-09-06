# zPermissions &mdash; A Superperms plugin for Bukkit #

This project exists mainly as a learning exercise and as a way to shakedown
my [ToHUtils](https://github.com/ZerothAngel/ToHUtils) project. Though
surprisingly, it is quite functional. :P

This plugin **only** supports Superperms. If you need a bridge for plugins
that use Permissions 2/3, please consider [PermissionsBukkit](http://dev.bukkit.org/server-mods/permbukkit/)'s
SuperpermsBridge.

## Features ##

*   Uses Bukkit database to store permissions (i.e. bukkit.yml). Note however,
    SQLite, the default database for Bukkit, does **not** work. You must use a
    real database. I know this will scare away 99% of prospective users. :P
	
    I developed with [H2](http://www.h2database.com/) and will probably deploy
	with it.

*   Group inheritance. Groups may inherit permissions from a single parent
	group.

*   Players may be members of more than one group. The order of which group
	permissions are applied is well defined and based on group priority
	(which is configurable, of course).

*   Multi-world support. Each group may define world-specific permissions.

*   Ranks! You may define multiple tracks on which to promote/demote users.

## To Do ##

I don't consider zPermissions feature complete yet (and thus not worthy of an
actual release or "1.0" version). Some things I would like to add:

*   Import/export to YAML. I realize the server (or in-game) command line is
	an awful interface. Being able to import/export would be nice for upgrades
	as well.

*   Possibly a YAML backend, for those afraid of using databases.
