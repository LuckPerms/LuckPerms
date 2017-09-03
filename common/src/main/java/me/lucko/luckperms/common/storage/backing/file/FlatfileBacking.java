/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.storage.backing.file;

import lombok.Getter;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.backing.AbstractBacking;
import me.lucko.luckperms.common.storage.backing.legacy.LegacyJSONSchemaMigration;
import me.lucko.luckperms.common.storage.backing.legacy.LegacyYAMLSchemaMigration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class FlatfileBacking extends AbstractBacking {
    private static final String LOG_FORMAT = "%s(%s): [%s] %s(%s) --> %s";

    private final Logger actionLogger = Logger.getLogger("luckperms_actions");
    private FileUuidCache uuidCache = new FileUuidCache();

    private final File pluginDir;

    @Getter
    private final String fileExtension;

    private final String dataFolderName;

    private File uuidData;
    private File actionLog;
    protected File usersDir;
    protected File groupsDir;
    protected File tracksDir;

    FlatfileBacking(LuckPermsPlugin plugin, String name, File pluginDir, String fileExtension, String dataFolderName) {
        super(plugin, name);
        this.pluginDir = pluginDir;
        this.fileExtension = fileExtension;
        this.dataFolderName = dataFolderName;
    }

    @Override
    public void init() {
        try {
            setupFiles();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        uuidCache.load(uuidData);

        try {
            FileHandler fh = new FileHandler(actionLog.getAbsolutePath(), 0, 1, true);
            fh.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return new Date(record.getMillis()).toString() + ": " + record.getMessage() + "\n";
                }
            });
            actionLogger.addHandler(fh);
            actionLogger.setUseParentHandlers(false);
            actionLogger.setLevel(Level.ALL);
            actionLogger.setFilter(record -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // schedule user cleanup
        plugin.getScheduler().asyncLater(this::cleanupUsers, 10L);

        setAcceptingLogins(true);
    }

    private void setupFiles() throws IOException {
        File data = new File(pluginDir, dataFolderName);

        // Try to perform schema migration
        File oldData = new File(pluginDir, "data");

        if (!data.exists() && oldData.exists()) {
            data.mkdirs();

            plugin.getLog().severe("===== Legacy Schema Migration =====");
            plugin.getLog().severe("Starting migration from legacy schema. This could take a while....");
            plugin.getLog().severe("Please do not stop your server while the migration takes place.");

            if (this instanceof YAMLBacking) {
                try {
                    new LegacyYAMLSchemaMigration(plugin, (YAMLBacking) this, oldData, data).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (this instanceof JSONBacking) {
                try {
                    new LegacyJSONSchemaMigration(plugin, (JSONBacking) this, oldData, data).run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            data.mkdirs();
        }

        usersDir = new File(data, "users");
        usersDir.mkdir();

        groupsDir = new File(data, "groups");
        groupsDir.mkdir();

        tracksDir = new File(data, "tracks");
        tracksDir.mkdir();

        uuidData = new File(data, "uuidcache.txt");
        uuidData.createNewFile();

        actionLog = new File(data, "actions.log");
        actionLog.createNewFile();

        // Listen for file changes.
        plugin.applyToFileWatcher(watcher -> {
            watcher.subscribe("users", usersDir.toPath(), s -> {
                if (!s.endsWith(fileExtension)) {
                    return;
                }

                String user = s.substring(0, s.length() - fileExtension.length());
                UUID uuid = Util.parseUuid(user);
                if (uuid == null) {
                    return;
                }

                User u = plugin.getUserManager().getIfLoaded(uuid);
                if (u != null) {
                    plugin.getLog().info("[FileWatcher] Refreshing user " + u.getFriendlyName());
                    plugin.getStorage().loadUser(uuid, "null");
                }
            });
            watcher.subscribe("groups", groupsDir.toPath(), s -> {
                if (!s.endsWith(fileExtension)) {
                    return;
                }

                String groupName = s.substring(0, s.length() - fileExtension.length());
                plugin.getLog().info("[FileWatcher] Refreshing group " + groupName);
                plugin.getUpdateTaskBuffer().request();
            });
            watcher.subscribe("tracks", tracksDir.toPath(), s -> {
                if (!s.endsWith(fileExtension)) {
                    return;
                }

                String trackName = s.substring(0, s.length() - fileExtension.length());
                plugin.getLog().info("[FileWatcher] Refreshing track " + trackName);
                plugin.getStorage().loadAllTracks();
            });
        });
    }

    @Override
    public void shutdown() {
        uuidCache.save(uuidData);
    }

    protected void registerFileAction(String type, File file) {
        plugin.applyToFileWatcher(fileWatcher -> fileWatcher.registerChange(type, file.getName()));
    }

    protected <T> T call(String file, Callable<T> c, T def) {
        try {
            return c.call();
        } catch (Exception e) {
            plugin.getLog().warn("Exception thrown whilst performing i/o: " + file);
            e.printStackTrace();
            return def;
        }
    }

    @Override
    public boolean logAction(LogEntry entry) {
        actionLogger.info(String.format(LOG_FORMAT,
                (entry.getActor().equals(Constants.CONSOLE_UUID) ? "" : entry.getActor() + " "),
                entry.getActorName(),
                Character.toString(entry.getType()),
                (entry.getActed() == null ? "" : entry.getActed().toString() + " "),
                entry.getActedName(),
                entry.getAction())
        );
        return true;
    }

    @Override
    public Log getLog() {
        // Flatfile doesn't support viewing log data from in-game. You can just read the file in a text editor.
        return Log.builder().build();
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        String[] fileNames = usersDir.list((dir, name) -> name.endsWith(fileExtension));
        if (fileNames == null) return null;
        return Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - fileExtension.length()))
                .map(UUID::fromString)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean loadAllGroups() {
        String[] fileNames = groupsDir.list((dir, name) -> name.endsWith(fileExtension));
        if (fileNames == null) return false;
        List<String> groups = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - fileExtension.length()))
                .collect(Collectors.toList());

        groups.forEach(this::loadGroup);

        GroupManager gm = plugin.getGroupManager();
        gm.getAll().values().stream()
                .filter(g -> !groups.contains(g.getName()))
                .forEach(gm::unload);
        return true;
    }

    @Override
    public boolean deleteGroup(Group group) {
        group.getIoLock().lock();
        try {
            return call(group.getName(), () -> {
                File groupFile = new File(groupsDir, group.getName() + fileExtension);
                registerFileAction("groups", groupFile);

                if (groupFile.exists()) {
                    groupFile.delete();
                }
                return true;
            }, false);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllTracks() {
        String[] fileNames = tracksDir.list((dir, name) -> name.endsWith(fileExtension));
        if (fileNames == null) return false;
        List<String> tracks = Arrays.stream(fileNames)
                .map(s -> s.substring(0, s.length() - fileExtension.length()))
                .collect(Collectors.toList());

        tracks.forEach(this::loadTrack);

        TrackManager tm = plugin.getTrackManager();
        tm.getAll().values().stream()
                .filter(t -> !tracks.contains(t.getName()))
                .forEach(tm::unload);
        return true;
    }

    @Override
    public boolean deleteTrack(Track track) {
        track.getIoLock().lock();
        try {
            return call(track.getName(), () -> {
                File trackFile = new File(tracksDir, track.getName() + fileExtension);
                registerFileAction("tracks", trackFile);

                if (trackFile.exists()) {
                    trackFile.delete();
                }
                return true;
            }, false);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        uuidCache.addMapping(username, uuid);
        return true;
    }

    @Override
    public UUID getUUID(String username) {
        return uuidCache.lookupUUID(username);
    }

    @Override
    public String getName(UUID uuid) {
        return uuidCache.lookupUsername(uuid);
    }
}
