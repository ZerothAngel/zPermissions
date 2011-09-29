# For Plugin Developers #

## Checking Group Membership ##

**There is no public API**

Let me say that again.

**THERE IS NO PUBLIC API** :P

However, as far as checking group membership, I strongly advocate the method hinted at by Dinnerbone in his [Permissions FAQ](http://forums.bukkit.org/threads/permissions-faq.25080/). Namely, using permissions to denote group membership.

zPermissions follows the convention put forth by the developers of WorldEdit, WorldGuard, etc., that is, check group membership by checking for a specific permission of the form:

    group.<groupname>

So a player in the `visitor` group will have the `group.visitor` permission automatically set to true. A player in the `admin` group will have the `group.admin` permission set to true, etc.

(And, of course, in zPermissions, the form of this permission is configurable &mdash; see the `group-permission` option in config.yml.)

Despite zPermissions doing this automatically, the beautiful thing about this convention is that *any* Superperms provider can provide the required permission. If necessary, server admins can just manually add the permission to each of their groups.

## Enumerating a Player's Groups ##

As above, zPermissions provides no public method to enumerate a player's groups. However, this can be easily done using only the Bukkit permissions API. For example:

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

And this is where the line ends. :P I have no plans to provide any sort of public API for changing group memberships. I absolutely abhor writing to implementations (see what it's like for a modern plugin to support the half dozen or so economy plugins... although that might be a bad example due to the existence of the Register API :P).

Unless Bukkit someday provides a groups management API (which seems like a bad idea, as it would be dictating an implementation detail to Superperms providers), the only supported method to programmatically modify group memberships in zPermissions **is through the command line**.

Excuting another plugin's commands is easy. And the awesome thing about using commands: you can make it configurable. Instead of compiling against X different plugins, just provide a command template that the server admin can easily change. For example, for zPermissions:

    permissions player %player% setgroup %group%

And the plugin would make the necessary substitutions before handing the command off to the server to be executed.

Actual implementation left up to any enterprising developers... ;)
