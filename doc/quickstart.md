# Quick Start #

## Storage Set-Up ##

zPermissions was primarily developed with using a (real) SQL database in mind. However, you can also opt to use it with a simple flat-file storage method. For SQL storage, zPermissions uses the persistence library included with CraftBukkit (called [Avaje Ebean](http://avaje.org/)). This persistence library is normally configured from the bukkit.yml file, under the `database` section.

The first thing to do is to decide how you want to set up zPermissions's permission storage: Will you be connecting to a database server? Use a flat-file? Or as another alternative, use an embedded database?

### Database Server ###

The most common free and open source database servers are [MySQL](http://dev.mysql.com/) and [PostgreSQL](http://www.postgresql.org/). Note that if you don't already have a basic understanding of setting up and administering these database servers, look and read their respective documentation. Alternatively, you might want to consider using the zPermissions flat-file approach.

Detailed instructions on configuring zPermissions with SQL database servers may be found at the [For Server Admins page](http://dev.bukkit.org/server-mods/zpermissions/pages/for-server-admins/).

### Flat-File Storage ###

To use flat-file storage, you must start your server with zPermissions at least once to create its config.yml. You can leave bukkit.yml as-is (i.e. configured for SQLite, which isn't compatible with zPermissions) since zPermissions will fall back to flat-file storage anyway. Once the config.yml has been created, set the following option to `false`:

    database-support: false

### Embedded Database ###

To use an embedded database (similar to SQLite), see the [H2](http://www.h2database.com/) instructions at the [For Server Admins page](http://dev.bukkit.org/server-mods/zpermissions/pages/for-server-admins/).

## ebean.properties SEVERE message ##

Note that you may see the following error in your logs:

    [SEVERE] ebean.properties not found

This is perfectly normal and harmless. But if you want to get rid of the message, simply create an empty file called `ebean.properties` in the same directory as bukkit.yml.

## The Default Group ##

The default group is determined by the `default-group` option in zPermissions's config.yml. Out-of-the-box, the default-group is named `default`. (If you change this option, be sure to `reload` your server or `/permissions reload` zPermissions.)

Regardless of whether or not you change the default group, when zPermissions starts up with a new database or a new flat-file, **it will have no groups defined.**

So one of the first things you'll probably want to do is create the default group.

If you left the default group alone:

    /permissions group default create

Or if you changed it, for example, to `guest`:

    /permissions group guest create

Once this is done, you can start assigning permissions to it.
