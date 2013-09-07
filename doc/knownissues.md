## Known Issues ##

*   **Towny compability**
    Towny does not support zPermissions, nor does it support Vault. My [pull request](https://github.com/ElgarL/Towny/pull/99) is in limbo. For now, you should be able to use the [SuperPerms method](http://palmergames.com/towny/towny-permission-nodes/#Miscellaneous_Nodes) for setting prefix/suffix and info/option nodes.

*   **XRay Informer**
    The current released version of XRay Informer (3.0.1) includes the Joda Time package in its jar. This causes NoClassDefFoundError exceptions with any Avaje Ebean-using plugin, of which zPermissions is one.
    [There is an open ticket regarding this at XRay Informer](http://dev.bukkit.org/bukkit-plugins/xray-informer/tickets/7-avoid-the-joda-embedded/).
