# Customizing Table Names #

If, for whatever reason, you cannot set up bukkit.yml to assign one database per plugin, you can choose to customize the names of zPermissions' tables instead. Doing so will help avoid table name collisions.

zPermissions has 9 tables. The tables and their default names are:

*   `ToHSchemaVersion` &mdash; zperms\_schema_version
*   `Entry` &mdash; entries
*   `Membership` &mdash; memberships
*   `PermissionEntity` &mdash; entities
*   `PermissionRegion` &mdash; regions
*   `PermissionWorld` &mdash; worlds
*   `EntityMetadata` &mdash; metadata
*   `Inheritance` &mdash; inheritances
*   `DataVersion` &mdash; data_version

To customize the names, simply add something like the following to your config.yml:

    tables:
      ToHSchemaVersion: zp_schema_version
      Entry: zp_entries
      Membership: zp_memberships
      PermissionEntity: zp_entities
      PermissionRegion: zp_regions
      PermissionWorld: zp_worlds
      EntityMetadata: zp_metadata
      Inheritance: zp_inheritances
      DataVersion: zp_data_version

(In this example, the default table names are prefixed with `zp_`.)

Note that zPermissions will only create the tables if it notices the `ToHSchemaVersion` or `PermissionEntity` tables missing. When changing table names, you should drop all old tables (or `ALTER TABLE ... RENAME TO ...` appropriately). You should change all table names in the configuration at once.

I have no plans to automate table renaming at all since it would put multi-server/shared-server schemas at risk.

Also, don't forget you can `/permissions import ...` and `/permissions export ...`, between databases and even between a database and the flat-file.

Lastly, the table names you specify in config.yml can actually be qualified using the following forms:

*   `catalog.schema.tableName`
*   `schema.tableName`
*   `tableName`

For example,

    tables:
      ToHSchemaVersion: myserver.zperms_schema_version
      Entry: myserver.entries
      Membership: myserver.memberships
      PermissionEntity: myserver.entities
      PermissionRegion: myserver.regions
      PermissionWorld: myserver.worlds
      EntityMetadata: myserver.metadata
      Inheritance: myserver.inheritances
      DataVersion: myserver.data_version

This would place all your tables in the `myserver` schema. Note that named schema support varies from database to database.
