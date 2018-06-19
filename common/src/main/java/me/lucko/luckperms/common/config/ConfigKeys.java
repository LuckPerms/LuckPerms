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

package me.lucko.luckperms.common.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LookupSetting;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.common.assignments.AssignmentRule;
import me.lucko.luckperms.common.config.keys.BooleanKey;
import me.lucko.luckperms.common.config.keys.CustomKey;
import me.lucko.luckperms.common.config.keys.EnduringKey;
import me.lucko.luckperms.common.config.keys.LowercaseStringKey;
import me.lucko.luckperms.common.config.keys.MapKey;
import me.lucko.luckperms.common.config.keys.StringKey;
import me.lucko.luckperms.common.graph.TraversalAlgorithm;
import me.lucko.luckperms.common.metastacking.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.metastacking.StandardStackElements;
import me.lucko.luckperms.common.model.TemporaryModifier;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.primarygroup.AllParentsByWeightHolder;
import me.lucko.luckperms.common.primarygroup.ParentsByWeightHolder;
import me.lucko.luckperms.common.primarygroup.PrimaryGroupHolder;
import me.lucko.luckperms.common.primarygroup.StoredHolder;
import me.lucko.luckperms.common.storage.SplitStorageType;
import me.lucko.luckperms.common.storage.StorageCredentials;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * All of the {@link ConfigKey}s used by LuckPerms.
 *
 * <p>The {@link #getAllKeys()} method and associated behaviour allows this class to behave
 * a bit like an enum with generics.</p>
 */
public final class ConfigKeys {

    /**
     * The name of the server
     */
    public static final ConfigKey<String> SERVER = CustomKey.of(c -> {
        if (c.getBoolean("use-server-properties-name", false)) {
            String serverName = c.getPlugin().getBootstrap().getServerName();
            if (serverName != null && !serverName.equals("Unknown Server")) {
                return serverName.toLowerCase();
            }
        }

        return c.getString("server", "global").toLowerCase();
    });

    /**
     * How many minutes to wait between syncs. A value <= 0 will disable syncing.
     */
    public static final ConfigKey<Integer> SYNC_TIME = EnduringKey.wrap(CustomKey.of(c -> {
        int val = c.getInt("sync-minutes", -1);
        if (val == -1) {
            val = c.getInt("data.sync-minutes", -1);
        }
        return val;
    }));

    /**
     * The lookup settings for contexts (care should be taken to not mutate this method)
     */
    public static final ConfigKey<EnumSet<LookupSetting>> LOOKUP_SETTINGS = CustomKey.of(c -> {
        return EnumSet.copyOf(Contexts.of(
                ContextSet.empty(),
                c.getBoolean("include-global", true),
                c.getBoolean("include-global-world", true),
                true,
                c.getBoolean("apply-global-groups", true),
                c.getBoolean("apply-global-world-groups", true),
                false
        ).getSettings());
    });

    /**
     * # If the servers own UUID cache/lookup facility should be used when there is no record for a player in the LuckPerms cache.
     */
    public static final ConfigKey<Boolean> USE_SERVER_UUID_CACHE = BooleanKey.of("use-server-uuid-cache", false);

    /**
     * If LuckPerms should allow usernames with non alphanumeric characters.
     */
    public static final ConfigKey<Boolean> ALLOW_INVALID_USERNAMES = BooleanKey.of("allow-invalid-usernames", false);

    /**
     * If LuckPerms should produce extra logging output when it handles logins.
     */
    public static final ConfigKey<Boolean> DEBUG_LOGINS = BooleanKey.of("debug-logins", false);

    /**
     * If LP should cancel login attempts for players whose permission data could not be loaded.
     */
    public static final ConfigKey<Boolean> CANCEL_FAILED_LOGINS = BooleanKey.of("cancel-failed-logins", false);

    /**
     * Controls how temporary add commands should behave
     */
    public static final ConfigKey<TemporaryModifier> TEMPORARY_ADD_BEHAVIOUR = CustomKey.of(c -> {
        String option = c.getString("temporary-add-behaviour", "deny").toLowerCase();
        if (!option.equals("deny") && !option.equals("replace") && !option.equals("accumulate")) {
            option = "deny";
        }

        return TemporaryModifier.valueOf(option.toUpperCase());
    });

    /**
     * How primary groups should be calculated.
     */
    public static final ConfigKey<String> PRIMARY_GROUP_CALCULATION_METHOD = EnduringKey.wrap(CustomKey.of(c -> {
        String option = c.getString("primary-group-calculation", "stored").toLowerCase();
        if (!option.equals("stored") && !option.equals("parents-by-weight") && !option.equals("all-parents-by-weight")) {
            option = "stored";
        }

        return option;
    }));

    /**
     * A function to create primary group holder instances based upon the {@link #PRIMARY_GROUP_CALCULATION_METHOD} setting.
     */
    public static final ConfigKey<Function<User, PrimaryGroupHolder>> PRIMARY_GROUP_CALCULATION = EnduringKey.wrap(CustomKey.of(c -> {
        String option = PRIMARY_GROUP_CALCULATION_METHOD.get(c);
        switch (option) {
            case "stored":
                return (Function<User, PrimaryGroupHolder>) StoredHolder::new;
            case "parents-by-weight":
                return (Function<User, PrimaryGroupHolder>) ParentsByWeightHolder::new;
            default:
                return (Function<User, PrimaryGroupHolder>) AllParentsByWeightHolder::new;
        }
    }));

    /**
     * If set to false, the plugin will allow a Users primary group to be removed with the
     * 'parent remove' command, and will set their primary group back to default.
     */
    public static final ConfigKey<Boolean> PREVENT_PRIMARY_GROUP_REMOVAL = BooleanKey.of("prevent-primary-group-removal", true);

    /**
     * If the plugin should check for "extra" permissions with users run LP commands
     */
    public static final ConfigKey<Boolean> USE_ARGUMENT_BASED_COMMAND_PERMISSIONS = BooleanKey.of("argument-based-command-permissions", false);

    /**
     * If wildcards are being applied
     */
    public static final ConfigKey<Boolean> APPLYING_WILDCARDS = EnduringKey.wrap(BooleanKey.of("apply-wildcards", true));

    /**
     * If regex permissions are being applied
     */
    public static final ConfigKey<Boolean> APPLYING_REGEX = EnduringKey.wrap(BooleanKey.of("apply-regex", true));

    /**
     * If shorthand permissions are being applied
     */
    public static final ConfigKey<Boolean> APPLYING_SHORTHAND = EnduringKey.wrap(BooleanKey.of("apply-shorthand", true));

    /**
     * If Bukkit child permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_BUKKIT_CHILD_PERMISSIONS = EnduringKey.wrap(BooleanKey.of("apply-bukkit-child-permissions", true));

    /**
     * If Bukkit default permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_BUKKIT_DEFAULT_PERMISSIONS = EnduringKey.wrap(BooleanKey.of("apply-bukkit-default-permissions", true));

    /**
     * If Bukkit attachment permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_BUKKIT_ATTACHMENT_PERMISSIONS = EnduringKey.wrap(BooleanKey.of("apply-bukkit-attachment-permissions", true));

    /**
     * If Nukkit child permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_NUKKIT_CHILD_PERMISSIONS = EnduringKey.wrap(BooleanKey.of("apply-nukkit-child-permissions", true));

    /**
     * If Nukkit default permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_NUKKIT_DEFAULT_PERMISSIONS = EnduringKey.wrap(BooleanKey.of("apply-nukkit-default-permissions", true));

    /**
     * If Nukkit attachment permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_NUKKIT_ATTACHMENT_PERMISSIONS = EnduringKey.wrap(BooleanKey.of("apply-nukkit-attachment-permissions", true));

    /**
     * If BungeeCord configured permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_BUNGEE_CONFIG_PERMISSIONS = EnduringKey.wrap(BooleanKey.of("apply-bungee-config-permissions", false));

    /**
     * If Sponge's implicit permission inheritance system should be applied
     */
    public static final ConfigKey<Boolean> APPLY_SPONGE_IMPLICIT_WILDCARDS = EnduringKey.wrap(BooleanKey.of("apply-sponge-implicit-wildcards", true));

    /**
     * If Sponge default subjects should be applied
     */
    public static final ConfigKey<Boolean> APPLY_SPONGE_DEFAULT_SUBJECTS = EnduringKey.wrap(BooleanKey.of("apply-sponge-default-subjects", true));

    /**
     * The algorithm LuckPerms should use when traversing the "inheritance tree"
     */
    public static final ConfigKey<TraversalAlgorithm> INHERITANCE_TRAVERSAL_ALGORITHM = CustomKey.of(c -> {
        String value = c.getString("inheritance-traversal-algorithm", "depth-first-pre-order");
        switch (value.toLowerCase()) {
            case "breadth-first":
                return TraversalAlgorithm.BREADTH_FIRST;
            case "depth-first-post-order":
                return TraversalAlgorithm.DEPTH_FIRST_POST_ORDER;
            default:
                return TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER;
        }
    });

    /**
     * The configured group weightings
     */
    public static final ConfigKey<Map<String, Integer>> GROUP_WEIGHTS = CustomKey.of(c -> {
        return c.getMap("group-weight", ImmutableMap.of()).entrySet().stream().collect(ImmutableCollectors.toMap(
                e -> e.getKey().toLowerCase(),
                e -> {
                    try {
                        return Integer.parseInt(e.getValue());
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
        );
    });

    /**
     * Creates a new prefix MetaStack element based upon the configured values.
     */
    public static final ConfigKey<MetaStackDefinition> PREFIX_FORMATTING_OPTIONS = CustomKey.of(l -> {
        List<String> format = l.getList("meta-formatting.prefix.format", new ArrayList<>());
        if (format.isEmpty()) {
            format.add("highest");
        }
        String startSpacer = l.getString("meta-formatting.prefix.start-spacer", "");
        String middleSpacer = l.getString("meta-formatting.prefix.middle-spacer", " ");
        String endSpacer = l.getString("meta-formatting.prefix.end-spacer", "");

        return new SimpleMetaStackDefinition(StandardStackElements.parseList(l.getPlugin(), format), startSpacer, middleSpacer, endSpacer);
    });

    /**
     * Creates a new suffix MetaStack element based upon the configured values.
     */
    public static final ConfigKey<MetaStackDefinition> SUFFIX_FORMATTING_OPTIONS = CustomKey.of(l -> {
        List<String> format = l.getList("meta-formatting.suffix.format", new ArrayList<>());
        if (format.isEmpty()) {
            format.add("highest");
        }
        String startSpacer = l.getString("meta-formatting.suffix.start-spacer", "");
        String middleSpacer = l.getString("meta-formatting.suffix.middle-spacer", " ");
        String endSpacer = l.getString("meta-formatting.suffix.end-spacer", "");

        return new SimpleMetaStackDefinition(StandardStackElements.parseList(l.getPlugin(), format), startSpacer, middleSpacer, endSpacer);
    });

    /**
     * If log notifications are enabled
     */
    public static final ConfigKey<Boolean> LOG_NOTIFY = BooleanKey.of("log-notify", true);

    /**
     * If auto op is enabled. Only used by the Bukkit platform.
     */
    public static final ConfigKey<Boolean> AUTO_OP = EnduringKey.wrap(BooleanKey.of("auto-op", false));

    /**
     * If server operators should be enabled. Only used by the Bukkit platform.
     */
    public static final ConfigKey<Boolean> OPS_ENABLED = EnduringKey.wrap(CustomKey.of(c -> !AUTO_OP.get(c) && c.getBoolean("enable-ops", true)));

    /**
     * If server operators should be able to use LuckPerms commands by default. Only used by the Bukkit platform.
     */
    public static final ConfigKey<Boolean> COMMANDS_ALLOW_OP = EnduringKey.wrap(BooleanKey.of("commands-allow-op", true));

    /**
     * If the vault server option should be used
     */
    public static final ConfigKey<Boolean> USE_VAULT_SERVER = BooleanKey.of("use-vault-server", true);

    /**
     * The name of the server to use for Vault.
     */
    public static final ConfigKey<String> VAULT_SERVER = CustomKey.of(c -> {
        // default to true for backwards compatibility
        if (USE_VAULT_SERVER.get(c)) {
            return c.getString("vault-server", "global").toLowerCase();
        } else {
            return SERVER.get(c);
        }
    });

    /**
     * If Vault should apply global permissions
     */
    public static final ConfigKey<Boolean> VAULT_INCLUDING_GLOBAL = BooleanKey.of("vault-include-global", true);

    /**
     * If any worlds provided with Vault lookups should be ignored
     */
    public static final ConfigKey<Boolean> VAULT_IGNORE_WORLD = BooleanKey.of("vault-ignore-world", false);

    /**
     * If Vault debug mode is enabled
     */
    public static final ConfigKey<Boolean> VAULT_DEBUG = BooleanKey.of("vault-debug", false);

    /**
     * The world rewrites map
     */
    public static final ConfigKey<Map<String, String>> WORLD_REWRITES = CustomKey.of(c -> {
        return c.getMap("world-rewrite", ImmutableMap.of()).entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> e.getKey().toLowerCase(),
                        e -> e.getValue().toLowerCase()
                ));
    });

    /**
     * The group name rewrites map
     */
    public static final ConfigKey<Map<String, String>> GROUP_NAME_REWRITES = MapKey.of("group-name-rewrite");

    /**
     * The default assignments being applied by the plugin
     */
    public static final ConfigKey<List<AssignmentRule>> DEFAULT_ASSIGNMENTS = CustomKey.of(c -> {
        return c.getKeys("default-assignments", ImmutableList.of()).stream().map(name -> {
            String hasTrue = c.getString("default-assignments." + name + ".if.has-true", null);
            String hasFalse = c.getString("default-assignments." + name + ".if.has-false", null);
            String lacks = c.getString("default-assignments." + name + ".if.lacks", null);
            List<String> give = ImmutableList.copyOf(c.getList("default-assignments." + name + ".give", ImmutableList.of()));
            List<String> take = ImmutableList.copyOf(c.getList("default-assignments." + name + ".take", ImmutableList.of()));
            String pg = c.getString("default-assignments." + name + ".set-primary-group", null);
            return new AssignmentRule(hasTrue, hasFalse, lacks, give, take, pg);
        }).collect(ImmutableCollectors.toList());
    });

    /**
     * The database settings, username, password, etc for use by any database
     */
    public static final ConfigKey<StorageCredentials> DATABASE_VALUES = EnduringKey.wrap(CustomKey.of(c -> {
        int maxPoolSize = c.getInt("data.pool-settings.maximum-pool-size", c.getInt("data.pool-size", 10));
        int minIdle = c.getInt("data.pool-settings.minimum-idle", maxPoolSize);
        int maxLifetime = c.getInt("data.pool-settings.maximum-lifetime", 1800000);
        int connectionTimeout = c.getInt("data.pool-settings.connection-timeout", 5000);
        Map<String, String> props = ImmutableMap.copyOf(c.getMap("data.pool-settings.properties", ImmutableMap.of("useUnicode", "true", "characterEncoding", "utf8")));

        return new StorageCredentials(
                c.getString("data.address", null),
                c.getString("data.database", null),
                c.getString("data.username", null),
                c.getString("data.password", null),
                maxPoolSize, minIdle, maxLifetime, connectionTimeout, props
        );
    }));

    /**
     * The prefix for any SQL tables
     */
    public static final ConfigKey<String> SQL_TABLE_PREFIX = EnduringKey.wrap(StringKey.of("data.table_prefix", "luckperms_"));

    /**
     * The prefix for any MongoDB collections
     */
    public static final ConfigKey<String> MONGODB_COLLECTION_PREFIX = EnduringKey.wrap(StringKey.of("data.mongodb_collection_prefix", ""));

    /**
     * MongoDB ClientConnectionURI to override default connection options
     */
    public static final ConfigKey<String> MONGODB_CONNECTION_URI = EnduringKey.wrap(StringKey.of("data.mongodb_connection_URI", ""));

    /**
     * The name of the storage method being used
     */
    public static final ConfigKey<String> STORAGE_METHOD = EnduringKey.wrap(LowercaseStringKey.of("storage-method", "h2"));

    /**
     * If storage files should be monitored for changes
     */
    public static final ConfigKey<Boolean> WATCH_FILES = BooleanKey.of("watch-files", true);

    /**
     * If split storage is being used
     */
    public static final ConfigKey<Boolean> SPLIT_STORAGE = EnduringKey.wrap(BooleanKey.of("split-storage.enabled", false));

    /**
     * The options for split storage
     */
    public static final ConfigKey<Map<SplitStorageType, String>> SPLIT_STORAGE_OPTIONS = EnduringKey.wrap(CustomKey.of(c -> {
        EnumMap<SplitStorageType, String> map = new EnumMap<>(SplitStorageType.class);
        map.put(SplitStorageType.USER, c.getString("split-storage.methods.user", "h2").toLowerCase());
        map.put(SplitStorageType.GROUP, c.getString("split-storage.methods.group", "h2").toLowerCase());
        map.put(SplitStorageType.TRACK, c.getString("split-storage.methods.track", "h2").toLowerCase());
        map.put(SplitStorageType.UUID, c.getString("split-storage.methods.uuid", "h2").toLowerCase());
        map.put(SplitStorageType.LOG, c.getString("split-storage.methods.log", "h2").toLowerCase());
        return ImmutableMap.copyOf(map);
    }));

    /**
     * The name of the messaging service in use, or "none" if not enabled
     */
    public static final ConfigKey<String> MESSAGING_SERVICE = EnduringKey.wrap(LowercaseStringKey.of("messaging-service", "none"));

    /**
     * If updates should be automatically pushed by the messaging service
     */
    public static final ConfigKey<Boolean> AUTO_PUSH_UPDATES = EnduringKey.wrap(BooleanKey.of("auto-push-updates", true));

    /**
     * If LuckPerms should push logging entries to connected servers via the messaging service
     */
    public static final ConfigKey<Boolean> PUSH_LOG_ENTRIES = EnduringKey.wrap(BooleanKey.of("push-log-entries", true));

    /**
     * If LuckPerms should broadcast received logging entries to players on this platform
     */
    public static final ConfigKey<Boolean> BROADCAST_RECEIVED_LOG_ENTRIES = EnduringKey.wrap(BooleanKey.of("broadcast-received-log-entries", false));

    /**
     * If redis messaging is enabled
     */
    public static final ConfigKey<Boolean> REDIS_ENABLED = EnduringKey.wrap(BooleanKey.of("redis.enabled", false));

    /**
     * The address of the redis server
     */
    public static final ConfigKey<String> REDIS_ADDRESS = EnduringKey.wrap(StringKey.of("redis.address", null));

    /**
     * The password in use by the redis server, or an empty string if there is no passworld
     */
    public static final ConfigKey<String> REDIS_PASSWORD = EnduringKey.wrap(StringKey.of("redis.password", ""));

    /**
     * The URL of the web editor
     */
    public static final ConfigKey<String> WEB_EDITOR_URL_PATTERN = StringKey.of("web-editor-url", "https://luckperms.github.io/editor/");

    /**
     * The URL of the verbose viewer
     */
    public static final ConfigKey<String> VERBOSE_VIEWER_URL_PATTERN = StringKey.of("verbose-viewer-url", "https://luckperms.github.io/verbose/");

    /**
     * The URL of the tree viewer
     */
    public static final ConfigKey<String> TREE_VIEWER_URL_PATTERN = StringKey.of("tree-viewer-url", "https://luckperms.github.io/treeview/");

    private static Map<String, ConfigKey<?>> KEYS = null;

    /**
     * Gets all of the possible config keys defined in this class
     *
     * @return all of the possible config keys defined in this class
     */
    public static synchronized Map<String, ConfigKey<?>> getAllKeys() {
        if (KEYS == null) {
            Map<String, ConfigKey<?>> keys = new LinkedHashMap<>();

            try {
                Field[] values = ConfigKeys.class.getFields();
                int counter = 0;

                for (Field f : values) {
                    // ignore non-static fields
                    if (!Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }

                    // ignore fields that aren't configkeys
                    if (!ConfigKey.class.equals(f.getType())) {
                        continue;
                    }

                    // get the key instance
                    BaseConfigKey<?> key = (BaseConfigKey<?>) f.get(null);
                    // set the ordinal value of the key.
                    key.ordinal = counter++;
                    // add the key to the return map
                    keys.put(f.getName(), key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            KEYS = ImmutableMap.copyOf(keys);
        }

        return KEYS;
    }

    private ConfigKeys() {}

}
