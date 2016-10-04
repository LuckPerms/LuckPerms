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

package me.lucko.luckperms.storage.methods;

import lombok.Cleanup;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.constants.Constants;
import me.lucko.luckperms.data.Log;
import me.lucko.luckperms.storage.Datastore;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import java.util.logging.Formatter;

abstract class FlatfileDatastore extends Datastore {
    private static final String LOG_FORMAT = "%s(%s): [%s] %s(%s) --> %s";

    private final Logger actionLogger = Logger.getLogger("lp_actions");
    private Map<String, String> uuidCache = new ConcurrentHashMap<>();

    private final File pluginDir;
    private File uuidData;
    private File actionLog;
    File usersDir;
    File groupsDir;
    File tracksDir;

    FlatfileDatastore(LuckPermsPlugin plugin, String name, File pluginDir) {
        super(plugin, name);
        this.pluginDir = pluginDir;
    }

    @Override
    public void init() {
        try {
            makeFiles();
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

    private void makeFiles() throws IOException {
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
    }

    @Override
    public void shutdown() {
        saveUUIDCache(uuidCache);
    }

    @Override
    public boolean logAction(LogEntry entry) {
        actionLogger.info(String.format(LOG_FORMAT,
                (entry.getActor().equals(Constants.getConsoleUUID()) ? "" : entry.getActor() + " "),
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

        try {
            @Cleanup FileReader fileReader = new FileReader(uuidData);
            @Cleanup BufferedReader bufferedReader = new BufferedReader(fileReader);

            Properties props = new Properties();
            props.load(bufferedReader);
            for (String key : props.stringPropertyNames()) {
                cache.put(key, props.getProperty(key));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return cache;
    }

    private void saveUUIDCache(Map<String, String> cache) {
        try {
            @Cleanup FileWriter fileWriter = new FileWriter(uuidData);
            @Cleanup BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            Properties properties = new Properties();
            properties.putAll(cache);
            properties.store(bufferedWriter, null);

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
