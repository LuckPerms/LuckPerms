/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.storage.backing;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

abstract class FlatfileBacking extends AbstractBacking {
    private static final String LOG_FORMAT = "%s(%s): [%s] %s(%s) --> %s";

    private final Logger actionLogger = Logger.getLogger("lp_actions");
    private Map<String, String> uuidCache = new ConcurrentHashMap<>();

    private final File pluginDir;
    private final String fileExtension;

    private File uuidData;
    private File actionLog;
    File usersDir;
    File groupsDir;
    File tracksDir;

    FlatfileBacking(LuckPermsPlugin plugin, String name, File pluginDir, String fileExtension) {
        super(plugin, name);
        this.pluginDir = pluginDir;
        this.fileExtension = fileExtension;
    }

    @Override
    public void init() {
        try {
            setupFiles();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        uuidCache.putAll(getUUIDCache());

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

        cleanupUsers();
        setAcceptingLogins(true);
    }

    private void setupFiles() throws IOException {
        File data = new File(pluginDir, "data");
        data.mkdirs();

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

                User u = plugin.getUserManager().get(uuid);
                if (u != null) {
                    plugin.getLog().info("[FileWatcher] Refreshing user " + u.getName());
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
        saveUUIDCache(uuidCache);
    }

    protected void registerFileAction(String type, File file) {
        plugin.applyToFileWatcher(fileWatcher -> fileWatcher.registerChange(type, file.getName()));
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

    private Map<String, String> getUUIDCache() {
        Map<String, String> cache = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(uuidData.toPath(), StandardCharsets.UTF_8)) {
            Properties props = new Properties();
            props.load(reader);
            for (String key : props.stringPropertyNames()) {
                cache.put(key, props.getProperty(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cache;
    }

    private void saveUUIDCache(Map<String, String> cache) {
        try (BufferedWriter writer = Files.newBufferedWriter(uuidData.toPath(), StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.putAll(cache);
            properties.store(writer, null);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        username = username.toLowerCase();
        uuidCache.put(username, uuid.toString());
        return true;
    }

    @Override
    public UUID getUUID(String username) {
        username = username.toLowerCase();
        if (uuidCache.get(username) == null) return null;
        return UUID.fromString(uuidCache.get(username));
    }

    @Override
    public String getName(UUID uuid) {
        for (Map.Entry<String, String> e : uuidCache.entrySet()) {
            if (e.getValue().equalsIgnoreCase(uuid.toString())) {
                return e.getKey();
            }
        }
        return null;
    }
}
