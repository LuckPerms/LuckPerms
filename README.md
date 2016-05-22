# LuckPerms
A quite possibly shit™ permissions implementation for Bukkit/BungeeCord.

### Why?
Yeah, I don't know. There are other more advanced, optimized, better programmed and thoroughly tested alternative plugins around, you should probably use those instead. I just wanted some specific features, and something that was compatible with BungeeCord.

### Features
 - **Group inheritance** - users can be members of multiple groups, groups can inherit other groups
 - **Multi-server support** - data is synced across all servers/platforms
 - **Per-server permissions/groups** - define permissions that only apply on certain servers
 - **Vault Support** - hooks into Vault to integrate with other plugins
 - **Everything is configured using commands** - no editing yml files, yuck
 - **Efficient** - maybe? Who knows, it might be.
 - **BungeeCord compatible** - my main motive for making this was that all other Bungee/Bukkit compatible perms plugins are utter aids. (At least, I couldn't find any decent ones)

### Caveats
 - Only supports MySQL
 - Not at all tested and could be super unreliable
 - It's quite possibly shit™

So, not anything major, really ¯\ _(ツ)_ /¯

### Commands
Command usage is printed when you supply too little arguments.

Bukkit: `/luckperms` `/perms` `/permissions` `/lp` `/perm`

Bungee: `/luckpermsbungee` `/bperms` `/bpermissions` `/lpb` `/bperm`
