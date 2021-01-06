![](https://i.imgur.com/ToguFkQ.png "Banner")
# LuckPerms
[![Build Status](https://ci.lucko.me/job/LuckPerms/badge/icon)](https://ci.lucko.me/job/LuckPerms/)
[![javadoc](https://javadoc.io/badge2/net.luckperms/api/javadoc.svg)](https://javadoc.io/doc/net.luckperms/api)
[![Maven Central](https://img.shields.io/maven-metadata/v/https/repo1.maven.org/maven2/net/luckperms/api/maven-metadata.xml.svg?label=maven%20central&colorB=brightgreen)](https://search.maven.org/artifact/net.luckperms/api)
[![Discord](https://img.shields.io/discord/241667244927483904.svg?label=discord&logo=discord)](https://discord.gg/luckperms)

LuckPerms is a permissions plugin for Minecraft servers. It allows server admins to control what features players can use by creating groups and assigning permissions.

The latest downloads, wiki & other useful links can be found on the project homepage at [luckperms.net](https://luckperms.net/).

It is:

* **fast** - written with performance and scalability in mind.
* **reliable** - trusted by thousands of server admins, and the largest of server networks.
* **easy to use** - setup permissions using commands, directly in config files, or using the web editor.
* **flexible** - supports a variety of data storage options, and works on lots of different server types.
* **extensive** - a plethora of customization options and settings which can be changed to suit your server.
* **free** - available for download and usage at no cost, and permissively licensed so it can remain free forever.

For more information, see the wiki article on [Why LuckPerms?](https://luckperms.net/wiki/Why-LuckPerms)

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
* **Bukkit, BungeeCord, Sponge, Fabric, Nukkit & Velocity** - Each use the common module to implement plugins on the respective server platforms.

## License
LuckPerms is licensed under the permissive MIT license. Please see [`LICENSE.txt`](https://github.com/lucko/LuckPerms/blob/master/LICENSE.txt) for more info.
