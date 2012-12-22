# For Server Admins #

## Database Setup ##

As mentioned, zPermissions does **NOT** work with SQLite, the default database used by CraftBukkit. If you would like something similar to SQLite (a Java-based, embedded database), I recommend [H2](http://www.h2database.com/).

Example bukkit.yml settings follow.

### MySQL ###

MySQL is probably the easiest database to use, especially since CraftBukkit already includes its driver. The relevant settings in your bukkit.yml would look something like:

	database:
		username: bukkit
		isolation: SERIALIZABLE
		driver: com.mysql.jdbc.Driver
		password: walrus
		url: jdbc:mysql://127.0.0.1:3306/{NAME}

If you do use the above URL (one database per plugin &mdash; extremely recommended), be sure to create a database for zPermissions before using it.

### H2 ###

Setting up CraftBukkit to use H2 is bit involved. One way to do it is:

1. Put h2.jar in the same directory as craftbukkit.jar

2. Change the way you launch CraftBukkit so you add h2.jar to the Java classpath. Something like:

        java {your-normal-Java-options} -cp craftbukkit.jar:h2.jar org.bukkit.craftbukkit.Main

3. Your bukkit.yml database settings would look something like:

        database:
         	username: bukkit
    		isolation: SERIALIZABLE
        	driver: org.h2.Driver
    		password: walrus
        	url: jdbc:h2:{DIR}{NAME}

### PostgreSQL ###

Setting up for PostgreSQL is similar to H2.

1. Put postgresql.jar in the same directory as craftbukkit.jar

2. Change the way you launch CraftBukkit so you add postgresql.jar to the Java classpath. Something like:

        java {your-normal-Java-options} -cp craftbukkit.jar:postgresql.jar org.bukkit.craftbukkit.Main

3. Your bukkit.yml database settings would look something like:

        database:
    		username: bukkit
        	isolation: SERIALIZABLE
    		driver: org.postgresql.Driver
        	password: walrus
    		url: jdbc:postgresql://127.0.0.1/{NAME}

As in the MySQL case, this configuration assigns a database to each plugin. Be sure to create one for zPermissions ahead of time and **remember** PostgreSQL's names are case-sensitive, so be sure to use quotes:

    create database 'zPermissions' with owner bukkit;

### Others ###

Other databases will probably work, provided it's supported by [Avaje Ebean](http://avaje.org/) and zPermissions's data model. The setup will probably be similar to PostgreSQL.

## Using a Test Database ##

If your test Bukkit server happens to use the same database server as your production Bukkit server, you can specify a different database by simply prefixing or suffixing {NAME} in the URL with a string of your choice. For example:

	database:
        ... omitted ...
		url: jdbc:mysql://127.0.0.1:3306/{NAME}_test

zPermissions on your test server would then use the database named zPermissions_test. Note that this will, of course, affect other plugins that also use Bukkit persistence.

## Permission Entry ##

The fact that zPermissions only has an SQL backend means there are only two ways to enter permissions:

1.  Through the command line (console or in-game... I recommend console)
2.  If you're masochistic enough, INSERTing directly into zPermissions's SQL tables.

I strongly recommend taking advantage of CraftBukkit's `permissions.yml` file (found in the server root), where you can define custom permissions with any number of children. For example:

	server.basics:
		description: Basic permissions for this server
		children:
			commandbook.motd: true
			commandbook.rules: true
			commandbook.say: true
			commandbook.say.me: true
			commandbook.who: true

With that, when you give the `server.basics` permission to a player or group, that player or group automatically gets those 5 commandbook permissions.

If you have a well-organized `permissions.yml`, you would ideally only need to assign a handful of permissions per group or player.

### There is Another... ###

An advanced alternative is to abuse/take advantage of zPermissions's import feature. zPermissions's dump files are nothing more than text files with zPermissions commands. So you can enter all your commands in a file ahead of time, place it in zPermissions's dump directory (by default, the `zPermissions-dumps` directory off the server root), and then run the `/permissions import <filename>` command.

But note, your database must be absolutely empty (no players or groups). So either purge everyone/everything or drop zPermissions's tables or database...
