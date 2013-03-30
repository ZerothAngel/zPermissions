# Customizing Table Names #

If, for whatever reason, you cannot set up bukkit.yml to assign one database per plugin, you can choose to customize the names of zPermissions' tables instead. Doing so will help avoid table name collisions.

zPermissions has 5 tables. The tables and their default names are:

*   `Entry` &mdash; entries
*   `Membership` &mdash; memberships
*   `PermissionEntity` &mdash; entities
*   `PermissionRegion` &mdash; regions
*   `PermissionWorld` &mdash; worlds

To customize the names, simply add something like the following to your config.yml:

    tables:
      Entry: zperms_entries
      Membership: zperms_memberships
      PermissionEntity: zperms_entities
      PermissionRegion: zperms_regions
      PermissionWorld: zperms_worlds

(In this example, the default table names are prefixed with `zperms_`.)

Note that zPermissions will only create the tables if it notices the `Entry` table missing. When changing table names, you should drop all old tables (or `ALTER TABLE ... RENAME TO ...` appropriately). You should change all table names in the configuration at once.

I have no plans to automate table renaming at all since it would put multi-server/shared-server schemas at risk.

Also, don't forget you can `/permissions import ...` and `/permissions export ...`, between databases and even between a database and the flat-file.

Lastly, the table names you specify in config.yml can actually be qualified using the following forms:

*   `catalog.schema.tableName`
*   `schema.tableName`
*   `tableName`

For example,

    tables:
      Entry: myserver.entries
      Membership: myserver.memberships
      PermissionEntity: myserver.entities
      PermissionRegion: myserver.regions
      PermissionWorld: myserver.worlds

This would place all your tables in the `myserver` schema. Note that named schema support varies from database to database.
