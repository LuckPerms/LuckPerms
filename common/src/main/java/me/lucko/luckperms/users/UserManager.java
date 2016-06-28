package me.lucko.luckperms.users;

import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class UserManager {

    /**
     * A {@link Map} containing all online/loaded users
     */
    @Getter
    private final Map<UUID, User> users = new ConcurrentHashMap<>();

    /**
     * Reference to main plugin instance
     */
    private final LuckPermsPlugin plugin;

    public UserManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a user object by UUID
     * @param uuid The uuid to search by
     * @return a {@link User} object if the user is loaded, returns null if the user is not loaded
     */
    public User getUser(UUID uuid) {
        return users.get(uuid);
    }

    /**
     * Get a user object by name
     * @param name The name to search by
     * @return a {@link User} object if the user is loaded, returns null if the user is not loaded
     */
    public User getUser(String name) {
        User user = null;

        for (User u : users.values()) {
            if (u.getName().equalsIgnoreCase(name)) {
                user = u;
                break;
            }
        }

        return user;
    }

    /**
     * Add a user to the {@link #getUsers()} map
     * @param user the user to add
     */
    public void setUser(User user) {
        users.put(user.getUuid(), user);
    }

    /**
     * Updates (or sets if the user wasn't already loaded) a user in the {@link #getUsers()} map
     * @param user The user to update or set
     */
    public void updateOrSetUser(User user) {
        if (!isLoaded(user.getUuid())) {
            // The user isn't already loaded, so we can just add
            users.put(user.getUuid(), user);
            // They're probably not online, but in case they are...
            user.refreshPermissions();
        } else {
            // Override the user's current loaded nodes, and force a refresh
            users.get(user.getUuid()).setNodes(user.getNodes());
            users.get(user.getUuid()).setPrimaryGroup(user.getPrimaryGroup());
            users.get(user.getUuid()).refreshPermissions();
        }
    }

    /**
     * Saves a user object in the datastore
     * @param user the user to save
     * @param datastore the datastore
     */
    public void saveUser(User user, Datastore datastore) {
        user.refreshPermissions();
        datastore.saveUser(user, success -> {});
    }

    /**
     * Set a user to the default group
     * @param user the user to give to
     */
    public void giveDefaults(User user) {
        // Setup the new user with default values
        try {
            user.setPermission(plugin.getConfiguration().getDefaultGroupNode(), true);
        } catch (ObjectAlreadyHasException ignored) {}
        user.setPrimaryGroup(plugin.getConfiguration().getDefaultGroupName());
    }

    /**
     * Check to see if a user is loaded or not
     * @param uuid the UUID of the user
     * @return true if the user is loaded
     */
    public boolean isLoaded(UUID uuid) {
        return users.containsKey(uuid);
    }

    /**
     * Removes and unloads any permission links of the user from the internal storage
     * @param user The user to unload
     */
    public abstract void unloadUser(User user);

    /**
     * Checks to see if the user is online, and if they are not, runs {@link #unloadUser(User)}
     * @param user The user to be cleaned up
     */
    public abstract void cleanupUser(User user);

    /**
     * Makes a new {@link User} object
     * @param uuid The UUID of the user
     * @return a new {@link User} object
     */
    public abstract User makeUser(UUID uuid);

    /**
     * Makes a new {@link User} object
     * @param uuid The UUID of the user
     * @param username The username of the user
     * @return a new {@link User} object
     */
    public abstract User makeUser(UUID uuid, String username);

    /**
     * Reloads the data of all online users
     */
    public abstract void updateAllUsers();
}
