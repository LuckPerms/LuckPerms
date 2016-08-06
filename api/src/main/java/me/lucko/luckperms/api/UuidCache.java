package me.lucko.luckperms.api;

import java.util.UUID;

/**
 * This UuidCache is a means of allowing users to have the same internal UUID across a network of offline mode servers
 * or mixed offline mode and online mode servers. Platforms running in offline mode generate a random UUID for a user when
 * they first join the server, but this UUID will then not be consistent across the network. LuckPerms will instead check
 * the datastore cache, to get a UUID for a user that is consistent across an entire network.
 *
 * If you want to get a user object from the datastore using the api on a server in offline mode, you will need to use this cache,
 * OR use Datastore#getUUID, for users that are not online.
 *
 * WARNING: THIS IS ONLY EFFECTIVE FOR ONLINE PLAYERS. USE THE DATASTORE METHODS FOR OFFLINE PLAYERS.
 */
public interface UuidCache {

    /**
     * Gets a users internal "LuckPerms" UUID, from the one given by the server.
     * @param external the UUID assigned by the server, through Player#getUniqueId or ProxiedPlayer#getUniqueId
     * @return the corresponding internal UUID
     */
    UUID getUUID(UUID external);

    /**
     * Gets a users external, server assigned or Mojang assigned unique id, from the internal one used within LuckPerms.
     * @param internal the UUID used within LuckPerms, through User#getUuid
     * @return the corresponding external UUID
     */
    UUID getExternalUUID(UUID internal);

}
