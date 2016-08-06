# LuckPerms [![Build Status](https://ci.lucko.me/job/LuckPerms/badge/icon)](https://ci.lucko.me/job/LuckPerms/)
A permissions implementation for Bukkit/Spigot, BungeeCord and Sponge.

## Links
* **Development Builds** - <https://ci.lucko.me/job/LuckPerms>
* **Javadocs** - <https://jd.lucko.me/LuckPerms>

## Features
* **Group inheritance** - users can be members of multiple groups, groups can inherit other groups
* **Temporary permissions** - users/groups can be given permissions that expire after a given time
* **Temporary groups** - users/groups can be added to/inherit other groups temporarily
* **Multi-server support** - data is synced across all servers/platforms
* **Full offline-mode/mixed-mode support** - player permissions are synced properly over offline-mode or mixed online/offline-mode networks.
* **Per-server permissions/groups** - define user/group permissions that only apply on certain servers
* **Per-world permissions/groups** - define user/group permissions that only apply on certain worlds (on BungeeCord, a connected Bukkit/Spigot instance is treated as a world)
* **Tracks / paths / ladders** - users can be promoted/demoted along multiple group tracks
* **Vault Support** - hooks into Vault to integrate with other plugins
* **Developer API** - easily integrate LuckPerms into your own projects
* **Easy and simple setup and configuration using commands** - no editing yml files, yuck
* **Negated permissions and groups** - define special rules for certain users/groups
* **Full support for UUIDs, even in Offline Mode** - users can change their usernames without losing permissions. In offline mode, a single user has the same internal UUID across a network.
* **Permission data stored within MySQL in a json format** - easily integrate the LuckPerms backend into your other projects
* **Well documented** - API methods have comprehensive Java docs, it's clear what each method does.
* **Efficient/lightweight** - maybe? Who knows, it might be.
* **Open Sourced, Free...** - you shouldn't have to pay $10+ for a "powerful" permissions plugin.
* **BungeeCord compatible** - permissions, users and groups are synced across all LuckPerms instances
* **Sponge compatible** - permissions, users and groups are synced across all LuckPerms instances (bukkit --> sponge, for example)
* **Support for MySQL, SQLite & Flatfile (JSON)** - other storage methods coming soon (maybe)

## Setup
All configuration options are in the **config.yml/luckperms.conf** file, which is generated automagically when the plugin first starts.

You can define the settings for per-server permissions, the storage method and credentials within this file.

## Info
### Permission Calculation
Permissions are calculated based on a priority system as follows.

* Temporary permissions will override non-temporary permissions.

Example: if a user has a false permission set for "test.node", and a temporary true permission set for "test.node", the temporary permission will override the permanent one, and the user will be granted the true node.

* World specific permissions will override generic permissions.

Example: if a user has a global "fly.use" permission, and then has a negated "fly.use" permission in the "world_nether" world, the world specific permission will override the globally defined one, and the user will be granted the negated node (provided they're in that world, of course.).

* Server specific permissions will override generic/global permissions.

Example: if a user has a global "fly.use" permission, and then has a negated "fly.use" permission on the "factions" server, the server specific permission will override the globally defined one, and the user will be granted the negated node.

* Inherited permissions will be overridden by an objects own permissions.

Example: A user is a member of the default group, which grants "some.thing.perm", but the users own permissions has "some.thing.perm" set to false. The inherited permission will be overridden by the users own permissions, and the user will be granted the negative node (provided they're on that server).

### Temporary Permissions
Temporary permissions are checked each time a user/group is loaded, and when the sync task runs. This means if you set a temporary permission to expire after 30 seconds, it won't actually be removed until the sync task runs.

The only way around this is to decrease the sync interval.

## API
LuckPerms has an extensive API, allowing for easy integration with other projects. To use the Api, you need to obtain an instance of the `LuckPermsApi` interface. This can be done in a number of ways.

```java
// On all platforms
final LuckPermsApi api = LuckPerms.getApi();

// On Bukkit/Spigot
final LuckPermsApi api = Bukkit.getServicesManager().getRegistration(LuckPermsApi.class).getProvider();

// On Sponge
Optional<LuckPermsApi> provider = Sponge.getServiceManager().provide(LuckPermsApi.class);
if (provider.isPresent()) {
    final LuckPermsApi api = provider.get();
}
```

If you want to use LuckPerms in your onEnable method, you need to add the following to your plugins `plugin.yml`.
```yml
depend: [LuckPerms]
```
All of the available methods can be seen in the various interfaces in the `luckperms-api` module.

You can add LuckPerms as a Maven dependency by adding the following to your projects `pom.xml`.
````xml
<repositories>
    <repository>
        <id>luck-repo</id>
        <url>https://repo.lucko.me/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>me.lucko.luckperms</groupId>
        <artifactId>luckperms-api</artifactId>
        <version>2.0</version>
    </dependency>
</dependencies>
````

## Versioning
As of version 2.0, LuckPerms roughly follows the standards set out in Semantic Versioning.

The only difference is that patch number is not included anywhere within the pom, and is calculated each build, based upon how may commits have been made since the last tag. (A new tag is made every minor version)

This means that API versions do not have a patch number (as no API changes are made in patches). API versions will be x.y, and each individual build of LuckPerms will follow x.y.z.

## Commands
Command usage is printed to the console/chat whenever invalid arguments are provided. Simply typing /perms will list all commands a user has permission to use.

### Aliases
| Bukkit           | Bungee           |
|------------------|------------------|
| /luckperms       | /luckpermsbungee |
| /perms           | /bperms          |
| /permissions     | /bpermissions    |
| /lp              | /lpb             |
| /perm            | /bperm           |

Arguments: \<required\> [optional]

Users with OP have access to all commands.

Additionally, you can use wildcards to grant users access to a selection of commands.
* **All commands** - luckperms.*
* **All user commands** - luckperms.user.*
* **All group commands** - luckperms.group.*
* **All track commands** - luckperms.track.*

### General
*  /perms - n/a
*  /perms sync - luckperms.sync
*  /perms info - luckperms.info
*  /perms debug - luckperms.debug
*  /perms creategroup \<group\> - luckperms.creategroup
*  /perms deletegroup \<group\> - luckperms.deletegroup
*  /perms listgroups - luckperms.listgroups
*  /perms createtrack \<track\> - luckperms.createtrack
*  /perms deletetrack \<track\> - luckperms.deletetrack
*  /perms listtracks - luckperms.listtracks

### User
*  /perms user \<user\> info - luckperms.user.info
*  /perms user \<user\> getuuid - luckperms.user.getuuid
*  /perms user \<user\> listnodes - luckperms.user.listnodes
*  /perms user \<user\> haspermission \<node\> [server] [world] - luckperms.user.haspermission
*  /perms user \<user\> inheritspermission \<node\> [server] [world] - luckperms.user.inheritspermission
*  /perms user \<user\> set \<node\> \<true/false\> [server] [world] - luckperms.user.setpermission
*  /perms user \<user\> unset \<node\> [server] [world] -  luckperms.user.unsetpermission
*  /perms user \<user\> addgroup \<group\> [server] [world] - luckperms.user.addgroup
*  /perms user \<user\> removegroup \<group\> [server] [world] - luckperms.user.removegroup
*  /perms user \<user\> settemp \<node\> \<true/false\> \<duration\> [server] [world] - luckperms.user.settemppermission
*  /perms user \<user\> unsettemp \<node\> [server] [world] - luckperms.user.unsettemppermission
*  /perms user \<user\> addtempgroup \<group\> \<duration\> [server] [world] - luckperms.user.addtempgroup
*  /perms user \<user\> removetempgroup \<group\> [server] [world] - luckperms.user.removetempgroup
*  /perms user \<user\> setprimarygroup \<group\> - luckperms.user.setprimarygroup
*  /perms user \<user\> showtracks - luckperms.user.showtracks
*  /perms user \<user\> promote \<track\> - luckperms.user.promote
*  /perms user \<user\> demote \<track\> - luckperms.user.demote
*  /perms user \<user\> showpos \<track\> - luckperms.user.showpos
*  /perms user \<user\> clear - luckperms.user.clear

### Group
*  /perms group \<group\> info - 	luckperms.group.info
*  /perms group \<group\> listnodes - luckperms.group.listnodes
*  /perms group \<group\> haspermission \<node\> [server] [world] - luckperms.group.haspermission
*  /perms group \<group\> inheritspermission \<node\> [server] [world] - luckperms.group.inheritspermission
*  /perms group \<group\> set \<node\> \<true/false\> [server] [world] - luckperms.group.setpermission
*  /perms group \<group\> unset \<node\> [server] [world] - luckperms.group.unsetpermission
*  /perms group \<group\> setinherit \<group\> [server] [world] - luckperms.group.setinherit
*  /perms group \<group\> unsetinherit \<group\> [server] [world] - luckperms.group.unsetinherit
*  /perms group \<group\> settemp \<node\> \<true/false\> \<duration\> [server] [world] - settemppermission
*  /perms group \<group\> unsettemp \<node\> [server] [world] - luckperms.group.unsettemppermission
*  /perms group \<group\> settempinherit \<group\> \<duration\> [server] [world] - luckperms.group.settempinherit
*  /perms group \<group\> unsettempinherit \<group\> [server] [world] - luckperms.group.unsettempinherit
*  /perms group \<group\> showtracks - luckperms.group.showtracks
*  /perms group \<group\> clear - luckperms.group.clear

### Track
*  /perms track \<track\> info - luckperms.track.info
*  /perms track \<track\> append \<group\> - luckperms.track.append
*  /perms track \<track\> insert \<group\> \<position\> - luckperms.track.insert
*  /perms track \<track\> remove \<group\> - luckperms.track.remove
*  /perms track \<track\> clear - luckperms.track.clear

## License
See LICENSE.md.