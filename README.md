# LuckPerms [![Build Status](https://ci.lucko.me/job/LuckPerms/badge/icon)](https://ci.lucko.me/job/LuckPerms/)
A permissions implementation for Bukkit/Spigot, BungeeCord and Sponge.

## Links
* **Development Builds** - <https://ci.lucko.me/job/LuckPerms>
* **Javadocs** - <https://jd.lucko.me/LuckPerms>
* **Wiki** - <https://github.com/lucko/LuckPerms/wiki>

## Why LuckPerms?
_Features checked on 19th Aug 2016. If you find any inaccuracies, please do let me know. I tried to be as fair as possible, and copy all major features from the respective plugin pages._

![alt text](https://static.lucko.me/luckperms-compare.png "Feature comparison")

## Features
* **Group inheritance** - users can be members of multiple groups, groups can inherit other groups
* **Temporary permissions** - users/groups can be given permissions that expire after a given time
* **Wildcard permissions** - users/groups can be given wildcard permissions (e.g. "minecraft.command.*"), even when plugins haven't implemented their own wildcards.
* **Temporary groups** - users/groups can be added to/inherit other groups temporarily
* **Multi-server support** - data is synced across all servers/platforms
* **Full offline-mode/mixed-mode support** - player permissions are synced properly over offline-mode or mixed online/offline-mode networks.
* **Per-server permissions/groups** - define user/group permissions that only apply on certain servers
* **Per-world permissions/groups** - define user/group permissions that only apply on certain worlds (on BungeeCord, a connected Bukkit/Spigot instance is treated as a world)
* **Tracks / paths / ladders** - users can be promoted/demoted along multiple group tracks
* **Vault Support** - hooks into Vault to integrate with other plugins
* **Developer API** - easily integrate LuckPerms into your own projects
* **Advanced action logging** - keep track of permission changes over time
* **Easily switch between storage systems** - export a log file from one datastore and import it into another
* **Easy and simple setup and configuration using commands** - no editing yml files, yuck
* **Negated permissions and groups** - define special rules for certain users/groups
* **Regex permissions** - define special permissions using regex
* **Shorthand nodes** - add nodes using the LuckPerms shorthand system
* **Full support for UUIDs, even in Offline Mode** - users can change their usernames without losing permissions. In offline mode, a single user has the same internal UUID across a network.
* **Permission data stored within MySQL in a json format** - easily integrate the LuckPerms backend into your other projects
* **Well documented** - API methods have comprehensive Java docs, it's clear what each method does.
* **Efficient/lightweight** - maybe? Who knows, it might be.
* **Open Sourced, Free...** - you shouldn't have to pay $10+ for a "powerful" permissions plugin.
* **BungeeCord compatible** - permissions, users and groups are synced across all LuckPerms instances
* **Sponge compatible** - permissions, users and groups are synced across all LuckPerms instances (bukkit --> sponge, for example)
* **Support for MySQL, MongoDB, SQLite, H2 & Flatfile (JSON)** - other storage methods coming soon (maybe)

## License
See LICENSE.md.