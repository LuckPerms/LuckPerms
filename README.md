![alt text](https://i.imgur.com/7TwJZ5e.png "Banner")
# LuckPerms [![Build Status](https://ci.lucko.me/job/LuckPerms/badge/icon)](https://ci.lucko.me/job/LuckPerms/) [![Javadocs](https://javadoc.io/badge/me.lucko.luckperms/luckperms-api.svg)](https://javadoc.io/doc/me.lucko.luckperms/luckperms-api) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/me.lucko.luckperms/luckperms-api/badge.svg)](https://maven-badges.herokuapp.com/maven-central/me.lucko.luckperms/luckperms-api)

LuckPerms is an advanced permissions implementation aiming to be a fast, reliable and flexible alternative to existing permission plugins. The project's main goals are centered around high performance and a wide feature set, filling the gaps of functionality and building upon existing features found in other plugins.

LuckPerms also includes an extensive API for developers, and support for a variety of Minecraft server software & data storage options.

Still not convinced? More information can be found on the wiki, under: [Why LuckPerms?](https://github.com/lucko/LuckPerms/wiki/Why-LuckPerms)

## Useful Links

The latest **downloads** can be found on the [project's homepage](https://luckperms.github.io/).

More information about the project & how to use the plugin can be found in the [wiki](https://github.com/lucko/LuckPerms/wiki).

## Building
LuckPerms uses Maven to handle dependencies.

#### Requirements
* Java 8 JDK or newer
* Maven 3.3.x or newer (the older versions will not work.)
* Git

#### Compiling from source
```sh
git clone https://github.com/lucko/LuckPerms.git
cd LuckPerms/
mvn clean package
```

You can find the output jars in the `target` directories.

## Contributing
#### Pull Requests
If you make any changes or improvements to the plugin which you think would be beneficial to others, please consider making a pull request to merge your changes back into the upstream project. (especially if your changes are bug fixes!)

LuckPerms loosely follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Generally, try to copy the style of code found in the class you're editing. 

#### Project Layout
The project is split up into a few seperate modules.

* **API** - The public, semantically versioned API used by other plugins wishing to integrate with and retrieve data from LuckPerms. This module (for the most part) does not contain any implementation itself, and is provided by the plugin.
* **Common** - The common module contains most of the code which implements the respective LuckPerms plugins. This abstract module reduces duplicated code throughout the project.
* **Bukkit, BungeeCord, Sponge & Nukkit** - Each use the common module to implement plugins on the respective server platforms.

## License
LuckPerms is licensed under the permissive MIT license. Please see [`LICENSE.txt`](https://github.com/lucko/LuckPerms/blob/master/LICENSE.txt) for more info.
