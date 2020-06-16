### :speech_balloon: Looking for support?

Details about support for the project can be found [here, on the Wiki Homepage](https://luckperms.net/wiki/Home#support).

### :bug: Reporting bugs?

Before reporting a bug or issue, please make sure that the issue is actually being caused by or related to LuckPerms. We get a lot of reports which are caused by other software - please double check!

If you're unsure, feel free to ask using the above resources BEFORE making a report.

Bugs or issues should be reported using the [GitHub Issues tab](https://github.com/lucko/LuckPerms/issues).

### :pencil: Want to contribute code?
#### Pull Requests
If you make any changes or improvements to the plugin which you think would be beneficial to others, please consider making a pull request to merge your changes back into the upstream project. (especially if your changes are bug fixes!)

LuckPerms loosely follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Generally, try to copy the style of code found in the class you're editing. 

If you're considering submitting a substantial pull request, please open an issue so we can discuss the change before starting work on the contribution. Most pull requests are happily accepted, but larger changes may have an impact on the maintainability of the project, and require more consideration. 

#### Project Layout
The project is split up into a few separate modules.

* **API** - The public, semantically versioned API used by other plugins wishing to integrate with and retrieve data from LuckPerms. This module (for the most part) does not contain any implementation itself, and is provided by the plugin.
* **Common** - The common module contains most of the code which implements the respective LuckPerms plugins. This abstract module reduces duplicated code throughout the project.
* **Bukkit, BungeeCord, Sponge, Nukkit, Velocity & Fabric** - Each use the common module to implement plugins on the respective server platforms.
