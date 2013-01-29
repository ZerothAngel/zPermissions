# For Plugin Developers #

## If you really must have an API... ##

Since 0.9.8, zPermissions has provided an API for common, read-only operations. It was originally developed with [Vault](http://dev.bukkit.org/server-mods/vault/) in mind, but of course, anyone can use it.

The API methods are exposed via the [ZPermissionsService](https://github.com/ZerothAngel/zPermissions/blob/master/src/main/java/org/tyrannyofheaven/bukkit/zPermissions/ZPermissionsService.java) interface.

First, you will need to add zPermissions-X.Y.Z.jar to your build path somehow. If your project uses Maven, add the following repository to your POM:

    <repository>
      <id>tyrannyofheaven.org</id>
      <url>http://maven.tyrannyofheaven.org/</url>
    </repository>

Then add the following dependency:

    <dependency>
      <groupId>org.tyrannyofheaven.bukkit</groupId>
      <artifactId>zPermissions</artifactId>
      <version>[0.9.8,)</version>
      <scope>provided</scope>
    </dependency>

If you don't use Maven (and why not?), you can visit my [Maven repository](http://maven.tyrannyofheaven.org/org/tyrannyofheaven/bukkit/zPermissions/) directly and download any version > 0.9.8.

Next, to get an actual implementation of this interface, your plugin should do something like the following:

    ZPermissionsService service = null;
    try {
        service = Bukkit.getServicesManager().load(ZPermissionsService.class);
    }
    catch (NoClassDefFoundError e) {
        // Eh...
    }
    if (service == null) {
        // zPermissions not found, do something else
    }

Ideally, you would do the lookup once (e.g. store the result in an instance or static variable) and, in addition to your plugin's onEnable(), possibly also perform the check inside a PluginEnableEvent handler. But the above is the general gist of it.

Note that if you are only checking for ZPermissionsService in your plugin's onEnable() method then you will need to add a line like the following to your plugin.yml:

    softdepend: [zPermissions]

Finally, if you look at the actual interface, you'll see that it only covers read-only operations. Operations that modify the state of zPermissions are still **only supported via the command line.** So you would probably use something like Bukkit.dispatchCommand().

What follows is the old (and of course, still supported!), non-zPermissions-specific Bukkit-only method of interacting with zPermissions...

## Checking Group Membership ##

As far as checking group membership, I strongly advocate the method hinted at by Dinnerbone in his [Permissions FAQ](http://forums.bukkit.org/threads/permissions-faq.25080/). Namely, using permissions to denote group membership.

zPermissions follows the convention put forth by the developers of WorldEdit, WorldGuard, etc., that is, check group membership by checking for a specific permission of the form:

    group.<groupname>

So a player in the `visitor` group will have the `group.visitor` permission automatically set to true. A player in the `admin` group will have the `group.admin` permission set to true, etc.

(And, of course, in zPermissions, the form of this permission is configurable &mdash; see the `group-permission` option in config.yml.)

Despite zPermissions doing this automatically, the beautiful thing about this convention is that *any* Superperms provider can provide the required permission. If necessary, server admins can just manually add the permission to each of their groups.

## Enumerating a Player's Groups ##

This can be easily done using only the Bukkit permissions API. For example:

    private static final String GROUP_PREFIX = "group.";

    ...

    public Set<String> getGroupsForPlayer(Player player) {
	    Set<String> groups = new HashSet<String>();
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (!pai.getPermission().startsWith(GROUP_PREFIX) || !pai.getValue())
                continue;
            groups.add(pai.getPermission().substring(GROUP_PREFIX.length()));
        }
        return groups;
	}

It is **STRONGLY** recommended that you actually make `GROUP_PREFIX` configurable. Do not hardcode it. This gives the server admin more flexibility.

## Changing Group Membership / Promoting / Demoting ##

I have no plans to provide any sort of public API for changing group memberships. I absolutely abhor writing to implementations (see what it's like for a modern plugin to support the half dozen or so economy plugins... although that might be a bad example due to the existence of Vault/Register API :P).

Unless Bukkit someday provides a groups management API (which seems like a bad idea, as it would be dictating an implementation detail to Superperms providers), the only supported method to programmatically modify group memberships in zPermissions **is through the command line**.

Excuting another plugin's commands is easy. And the awesome thing about using commands: you can make it configurable. Instead of compiling against X different plugins, just provide a command template that the server admin can easily change. For example, for zPermissions:

    permissions player %player% setgroup %group%

And the plugin would make the necessary substitutions before handing the command off to the server to be executed.

Actual implementation left up to any enterprising developers... ;)
