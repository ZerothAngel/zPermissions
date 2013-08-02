## Known Issues ##

*   **Essentials compatibility**
    The current released version of Essentials (2.11.1) does not recognize zPermissions as a permissions plugin. By default, it will fall back to using the "player-commands" section in its config.yml to determine access to commands. To get Essentials to instead use (SuperPerms) permissions, remove the entire "player-commands" section from its config.yml.

    A future version of Essentials should support zPermissions since [my pull request](https://github.com/essentials/Essentials/pull/494) was accepted shortly after the 2.11.1 release.

*   **Towny compability**
    Towny does not support zPermissions, nor does it support Vault. My [pull request](https://github.com/ElgarL/Towny/pull/99) is in limbo. For now, you should be able to use the [SuperPerms method](http://palmergames.com/towny/towny-permission-nodes/#Miscellaneous_Nodes) for setting prefix/suffix and info/option nodes.
