## Temporary Permissions & Groups ##

zPermissions supports two methods of giving players temporary permissions: the settemp command and temporary group assignments.

### settemp ###

The settemp command looks like so:

    /permissions player <player> settemp [-t <timeout>] <permission> [value]

The timeout is in seconds and, if omitted, will default to the value specified in config.yml. Ideally, the timeout should be something short &mdash; a few seconds to a few minutes. The reason being that settemp is implemented using Bukkit's own support for temporary permissions. Which means that these temporary permissions are truly temporary &mdash; they will disappear when any of the following happens: the player logs off, the server is restarted, and of course, when they expire.

## Temporary Group Assignments ##

The group..add command will take an optional duration or timestamp:

    /permissions group <group> add <player> [<duration/timestamp>]

As will the player..setgroup command:

    /permissions player <player> setgroup <group> [<duration/timestamp>]

The duration defaults to days and may be modified by adding any of the following suffixes:

<table>
<tr><th>Suffix</th><th>Units</th></tr>
<tr><td>min, mins, minute, minutes</td><td>Minutes</td></tr>
<tr><td>h, hour, hours</td><td>Hours</td></tr>
<tr><td>d, day, days</td><td>Days</td></tr>
<tr><td>m, month, months</td><td>Months</td></tr>
<tr><td>y, year, years</td><td>Years</td></tr>
</table>

A timestamp takes either of the following forms:

<table>
<tr><th>Format</th><th>Example</th></tr>
<tr><td>YYYY-MM-DDThh:mm</td><td>2013-06-14T19:46</td></tr>
<tr><td>YYYY-MM-DD</td><td>2013-06-14</td></tr>
</table>
(In reality, it will take any [ISO 8601](http://en.wikipedia.org/wiki/ISO_8601) formatted timestamp including seconds and timezone offset.)

If you use temporary group assignments in lieu of (settemp) temporary permissions (maybe because you want the permissions to persist until expiration, regardless of the player logging off or server restarting), you might consider setting the weight of the group to some negative number so it doesn't intefere with the primary group determination [as described here](http://dev.bukkit.org/bukkit-plugins/zpermissions/pages/vault-support/). This might only be needed if you use a chat formatting plugin with zPermissions as your source of prefixes/suffixes.
