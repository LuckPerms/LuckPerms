package me.lucko.luckperms.data;

import lombok.Getter;
import lombok.Setter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;

import java.util.UUID;

public abstract class Datastore {
    protected final LuckPermsPlugin plugin;

    @Getter
    public final String name;

    @Getter
    @Setter
    private boolean acceptingLogins;

    protected Datastore(LuckPermsPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.acceptingLogins = true;
    }

    /**
     * Execute a runnable asynchronously
     * @param r the task to run
     */
    private void doAsync(Runnable r) {
        plugin.doAsync(r);
    }

    /**
     * Execute a runnable synchronously
     * @param r the task to run
     */
    private void doSync(Runnable r) {
        plugin.doSync(r);
    }

    private void runCallback(boolean result, Callback callback) {
        doSync(() -> callback.onComplete(result));
    }

    /*
        These methods will block the thread that they're ran on.
     */
    public abstract void init();
    public abstract void shutdown();
    public abstract boolean loadOrCreateUser(UUID uuid, String username);
    public abstract boolean loadUser(UUID uuid);
    public abstract boolean saveUser(User user);
    public abstract boolean createAndLoadGroup(String name);
    public abstract boolean loadGroup(String name);
    public abstract boolean loadAllGroups();
    public abstract boolean saveGroup(Group group);
    public abstract boolean deleteGroup(Group group);
    public abstract boolean saveUUIDData(String username, UUID uuid);
    public abstract UUID getUUID(String username);



    /*
        These methods will return as soon as they are called. The callback will be ran when the task is complete
        They therefore will not block the thread that they're ran on

        Callbacks are ran on the main server thread (if applicable)
     */
    public void loadOrCreateUser(UUID uuid, String username, Callback callback) {
        doAsync(() -> runCallback(loadOrCreateUser(uuid, username), callback));
    }

    public void loadUser(UUID uuid, Callback callback) {
        doAsync(() -> runCallback(loadUser(uuid), callback));
    }

    public void saveUser(User user, Callback callback) {
        doAsync(() -> runCallback(saveUser(user), callback));
    }

    public void createAndLoadGroup(String name, Callback callback) {
        doAsync(() -> runCallback(createAndLoadGroup(name), callback));
    }

    public void loadGroup(String name, Callback callback) {
        doAsync(() -> runCallback(loadGroup(name), callback));
    }

    public void loadAllGroups(Callback callback) {
        doAsync(() -> runCallback(loadAllGroups(), callback));
    }

    public void saveGroup(Group group, Callback callback) {
        doAsync(() -> runCallback(saveGroup(group), callback));
    }

    public void deleteGroup(Group group, Callback callback) {
        doAsync(() -> runCallback(deleteGroup(group), callback));
    }

    public void saveUUIDData(String username, UUID uuid, Callback callback) {
        doAsync(() -> runCallback(saveUUIDData(username, uuid), callback));
    }

    public void getUUID(String username, GetUUIDCallback callback) {
        doAsync(() -> doSync(() -> callback.onComplete(getUUID(username))));
    }

    public interface Callback {
        void onComplete(boolean success);
    }

    public interface GetUUIDCallback {
        void onComplete(UUID uuid);
    }
}
