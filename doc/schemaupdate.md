## Schema Updates ##

On rare occassions, the current version of zPermissions may perform a schema update. (**Read the change log to see if this is the case.**) If you're using the file-based storage, then none of this applies to you. If you're using a database, then read on.

**I will only support automated schema updates for H2, MySQL, and PostgreSQL.** That's not to say that zPermissions doesn't work with database X; [Avaje Ebean](http://www.avaje.org/) supports a wide range of databases and zPermissions will probably happily work with most of them. However, as far as developing & testing goes, H2, MySQL, and PostgreSQL are all I have access to. 

**Regardless of which database you use, you should back up your permissions before running a new version of zPermissions that will update your schema.** (Again, the change log will clearly say if the current version will do this.) You can make a backup by performing an export like so:

    /permissions export <some-filename>

This will create an export file (which is nothing more than a list of commands to re-create your permissions set up) in the configured dump directory. By default, this is `zPermissions-dumps` in your server directory.

Alternatively (or additionally), you can make an SQL backup as well. How you do that depends on what database you're using. (And you're advised to look at your database administration docs for more details...)

### Unsupported Databases ###

If you have an unsupported database (something other than the 3 I mentioned), then you pretty much only have one choice:

1. Perform a `/permissions export` with the prior version of zPermissions
2. DROP all zPermissions tables, indexes, sequences, etc.
3. Run the new version of zPermissions
4. Perform a `/permissions import`

(A more advanced alternative is to manually update your schema. Depending on what changes, this can be very simple to very complicated. You're almost better off doing the above steps. But here's a hint anyway: Look in the `sql/common` directory in the zPermissions JAR file.)

### Schema Version History ###

<table>
  <tr>
    <th>Version</th>
    <th>zPerms Version</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>1</td>
    <td>0.9.17 and earlier</td>
    <td>Initial schema</td>
  <tr>
  <tr>
    <td>2</td>
    <td>0.9.18</td>
    <td>Adds expiration column to Membership table</td>
  <tr>
  <tr>
    <td>3</td>
    <td>0.9.18</td>
    <td>Adds EntityMetadata table</td>
  <tr>
  <tr>
    <td>4</td>
    <td>1.0</td>
    <td>Adds Inheritance table</td>
  </tr>
  <tr>
    <td>5</td>
    <td>1.1</td>
    <td>Adds DataVersion table</td>
  </tr>
</table>
