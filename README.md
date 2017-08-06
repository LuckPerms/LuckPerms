![alt text](https://i.imgur.com/7TwJZ5e.png "Banner")
# LuckPerms [![Build Status](https://ci.lucko.me/job/LuckPerms/badge/icon)](https://ci.lucko.me/job/LuckPerms/)
LuckPerms is an advanced permissions implementation aiming to be a fast, reliable and flexible alternative to existing permission plugins. The project's main goals are centered around high performance and a wide feature set, filling the gaps of functionality and building upon existing features found in other plugins.

LuckPerms also includes an extensive API for developers, and support for a variety of Minecraft server software & data storage options.

[See the charts below](https://github.com/lucko/LuckPerms#luckperms-vs-other-plugins) for a partial list of features & comparison with other providers.

* [Spigot Plugin Release Page](https://www.spigotmc.org/resources/luckperms-an-advanced-permissions-system.28140/ "Spigot Plugin Page")
* [Sponge Plugin Release Page](https://forums.spongepowered.org/t/luckperms-an-advanced-permissions-system/14274 "Sponge Plugin Page")

## Useful Links
* **Development Builds** - <https://ci.lucko.me/job/LuckPerms>
* **Javadocs** - <https://luckperms.lucko.me/javadocs/>
* **Wiki** - <https://github.com/lucko/LuckPerms/wiki>
* **Wiki(Simplified Chinese)** - <https://github.com/PluginsCDTribe/LuckPerms/wiki>

## Building
LuckPerms uses Maven to handle dependencies.

#### Requirements
* Java 8 JDK
* Maven 3.3.x (the older versions will not work.)
* Git

#### Then run
```sh
git clone https://github.com/lucko/LuckPerms.git
cd LuckPerms/
mvn clean package
```

You can find the output jars in the `target` directories.

## Contributing
In order to contribute to and make changes to the plugin, you will need the dependencies listed above, plus the [Lombok plugin](https://projectlombok.org/download.html) for your IDE. 

#### Pull Requests
If you make any changes or improvements to the plugin which you think would be beneficial upstream, please consider making a Pull Request to merge your changes back into the upstream project. (especially if your changes are bug fixes!)

LuckPerms loosely follows the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), with a few minor changes here and there. Generally, try to copy the style of code found in the class you're editing. 

#### Project Layout
The project is split up into 5 seperate modules.

* **API** - The public facing API classes used by other plugins when integrating with LuckPerms. This is basically just a collection of interfaces implemented inside of the plugin.
* **Common** - This includes all of the classes shared by separate implementations of LuckPerms. This is where the bulk of the project is.
* **Bukkit, BungeeCord & Sponge** - Includes all of the platform specific classes used to implement LuckPerms on each of the 3 supported platforms.

## License
LuckPerms is licensed under the permissive MIT license. Please see [`LICENSE.txt`](https://github.com/lucko/LuckPerms/blob/master/LICENSE.txt) for more info.

## LuckPerms vs Other Plugins
### Bukkit
![alt text](https://luckperms.lucko.me/assets/bukkit-compare.png "Feature comparison")

### Sponge
![alt text](https://luckperms.lucko.me/assets/sponge-compare.png "Feature comparison")


## Thanks
[![JProfiler](https://www.ej-technologies.com/images/product_banners/jprofiler_large.png)](http://www.ej-technologies.com/products/jprofiler/overview.html)

Thanks to ej-technologies for granting LuckPerms an open source licence to their [Java Profiling Software](http://www.ej-technologies.com/products/jprofiler/overview.html "Java Profiler").
