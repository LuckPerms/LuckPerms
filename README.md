![alt text](https://i.imgur.com/ToguFkQ.png "Banner")
# LuckPerms
[![Build Status](https://ci.lucko.me/job/LuckPerms/badge/icon)](https://ci.lucko.me/job/LuckPerms/)
[![javadoc](https://javadoc.io/badge2/net.luckperms/api/javadoc.svg)](https://javadoc.io/doc/net.luckperms/api)
[![Maven Central](https://img.shields.io/maven-metadata/v/https/repo1.maven.org/maven2/net/luckperms/api/maven-metadata.xml.svg?label=maven%20central&colorB=brightgreen)](https://search.maven.org/artifact/net.luckperms/api)
[![Discord](https://img.shields.io/discord/241667244927483904.svg?logo=discord&label=)](https://discord.gg/luckperms)

LuckPerms is an advanced permissions implementation aiming to be a fast, reliable and flexible alternative to existing permission plugins. The project's main goals are centered around high performance and a wide feature set, filling the gaps of functionality and building upon existing features found in other plugins.

LuckPerms also includes an extensive API for developers, and support for a variety of Minecraft server software & data storage options.

Still not convinced? More information can be found on the wiki, under: [Why LuckPerms?](https://github.com/lucko/LuckPerms/wiki/Why-LuckPerms)

## Useful Links

The latest **downloads** can be found on the [project's homepage](https://luckperms.net/).

More information about the project & how to use the plugin can be found in the [wiki](https://github.com/lucko/LuckPerms/wiki).

## Building
LuckPerms uses Gradle to handle dependencies & building.

#### Requirements
* Java 8 JDK or newer
* Git

#### Compiling from source
```sh
git clone https://github.com/lucko/LuckPerms.git
cd LuckPerms/
./gradlew build
```

You can find the output jars in the `build/libs` directories.

## Contributing
#### Pull Requests
If you make any changes or improvements to the plugin which you think would be beneficial to others, please consider making a pull request to merge your changes back into the upstream project. (especially if your changes are bug fixes!)

LuckPerms loosely follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html). Generally, try to copy the style of code found in the class you're editing. 

#### Project Layout
The project is split up into a few separate modules.

* **API** - The public, semantically versioned API used by other plugins wishing to integrate with and retrieve data from LuckPerms. This module (for the most part) does not contain any implementation itself, and is provided by the plugin.
* **Common** - The common module contains most of the code which implements the respective LuckPerms plugins. This abstract module reduces duplicated code throughout the project.
* **Bukkit, BungeeCord, Sponge, Nukkit & Velocity** - Each use the common module to implement plugins on the respective server platforms.

## License
LuckPerms is licensed under the permissive MIT license. Please see [`LICENSE.txt`](https://github.com/lucko/LuckPerms/blob/master/LICENSE.txt) for more info.
