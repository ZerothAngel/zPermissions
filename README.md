# zPermissions &mdash; A Superperms plugin for Bukkit #

This project exists mainly as a learning exercise and as a way to shakedown
my [ToHUtils](https://github.com/ZerothAngel/ToHUtils) project. Though
surprisingly, it is quite functional. :P

This plugin **only** supports Superperms. If you need a bridge for plugins
that use Permissions 2/3, please consider [PermissionsBukkit](http://dev.bukkit.org/server-mods/permbukkit/)'s
SuperpermsBridge.

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
    regions. For now, you must be using a post-5.2.2 development version
	of [WorldGuard](http://build.sk89q.com/job/WorldGuard/).
		
*   Ranks! You may define multiple tracks on which to promote/demote users.

## Usage ##

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

## To Do ##

I don't consider zPermissions feature complete yet (and thus not worthy of an
actual release or "1.0" version). Some things I would like to add:

*   Import/export to YAML. I realize the server (or in-game) command line is
	an awful interface. Being able to import/export would be nice for upgrades
	as well.

*   Possibly a YAML backend, for those afraid of using databases.
