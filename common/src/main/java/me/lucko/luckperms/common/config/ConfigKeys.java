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

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.command.utils.ArgumentParser;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.graph.TraversalAlgorithm;
import me.lucko.luckperms.common.metastacking.SimpleMetaStackDefinition;
import me.lucko.luckperms.common.metastacking.StandardStackElements;
import me.lucko.luckperms.common.model.PrimaryGroupHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.implementation.split.SplitStorageType;
import me.lucko.luckperms.common.storage.misc.StorageCredentials;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import net.luckperms.api.metastacking.DuplicateRemovalFunction;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryOptions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static me.lucko.luckperms.common.config.ConfigKeyTypes.booleanKey;
import static me.lucko.luckperms.common.config.ConfigKeyTypes.customKey;
import static me.lucko.luckperms.common.config.ConfigKeyTypes.enduringKey;
import static me.lucko.luckperms.common.config.ConfigKeyTypes.lowercaseStringKey;
import static me.lucko.luckperms.common.config.ConfigKeyTypes.mapKey;
import static me.lucko.luckperms.common.config.ConfigKeyTypes.stringKey;

/**
 * All of the {@link ConfigKey}s used by LuckPerms.
 *
 * <p>The {@link #getKeys()} method and associated behaviour allows this class
 * to function a bit like an enum, but with generics.</p>
 */
public final class ConfigKeys {
    private ConfigKeys() {}

    /**
     * The name of the server
     */
    public static final ConfigKey<String> SERVER = lowercaseStringKey("server", "global");

    /**
     * How many minutes to wait between syncs. A value <= 0 will disable syncing.
     */
    public static final ConfigKey<Integer> SYNC_TIME = enduringKey(customKey(c -> {
        int val = c.getInteger("sync-minutes", -1);
        if (val == -1) {
            val = c.getInteger("data.sync-minutes", -1);
        }
        return val;
    }));

    /**
     * The default global contexts instance
     */
    public static final ConfigKey<QueryOptions> GLOBAL_QUERY_OPTIONS = customKey(c -> {
        Set<Flag> flags = EnumSet.of(Flag.RESOLVE_INHERITANCE);
        if (c.getBoolean("include-global", true)) {
            flags.add(Flag.INCLUDE_NODES_WITHOUT_SERVER_CONTEXT);
        }
        if (c.getBoolean("include-global-world", true)) {
            flags.add(Flag.INCLUDE_NODES_WITHOUT_WORLD_CONTEXT);
        }
        if (c.getBoolean("apply-global-groups", true)) {
            flags.add(Flag.APPLY_INHERITANCE_NODES_WITHOUT_SERVER_CONTEXT);
        }
        if (c.getBoolean("apply-global-world-groups", true)) {
            flags.add(Flag.APPLY_INHERITANCE_NODES_WITHOUT_WORLD_CONTEXT);
        }
        return QueryOptions.contextual(ImmutableContextSetImpl.EMPTY, flags);
    });

    /**
     * # If the servers own UUID cache/lookup facility should be used when there is no record for a player in the LuckPerms cache.
     */
    public static final ConfigKey<Boolean> USE_SERVER_UUID_CACHE = booleanKey("use-server-uuid-cache", false);

    /**
     * If LuckPerms should allow usernames with non alphanumeric characters.
     */
    public static final ConfigKey<Boolean> ALLOW_INVALID_USERNAMES = booleanKey("allow-invalid-usernames", false);

    /**
     * If LuckPerms should produce extra logging output when it handles logins.
     */
    public static final ConfigKey<Boolean> DEBUG_LOGINS = booleanKey("debug-logins", false);

    /**
     * If LP should cancel login attempts for players whose permission data could not be loaded.
     */
    public static final ConfigKey<Boolean> CANCEL_FAILED_LOGINS = booleanKey("cancel-failed-logins", false);

    /**
     * Controls how temporary add commands should behave
     */
    public static final ConfigKey<TemporaryNodeMergeStrategy> TEMPORARY_ADD_BEHAVIOUR = customKey(c -> {
        String option = c.getString("temporary-add-behaviour", "deny").toLowerCase();
        if (!option.equals("deny") && !option.equals("replace") && !option.equals("accumulate")) {
            option = "deny";
        }

        return ArgumentParser.parseTemporaryModifier(option);
    });

    /**
     * How primary groups should be calculated.
     */
    public static final ConfigKey<String> PRIMARY_GROUP_CALCULATION_METHOD = enduringKey(customKey(c -> {
        String option = c.getString("primary-group-calculation", "stored").toLowerCase();
        if (!option.equals("stored") && !option.equals("parents-by-weight") && !option.equals("all-parents-by-weight")) {
            option = "stored";
        }

        return option;
    }));

    /**
     * A function to create primary group holder instances based upon the {@link #PRIMARY_GROUP_CALCULATION_METHOD} setting.
     */
    public static final ConfigKey<Function<User, PrimaryGroupHolder>> PRIMARY_GROUP_CALCULATION = enduringKey(customKey(c -> {
        String option = PRIMARY_GROUP_CALCULATION_METHOD.get(c);
        switch (option) {
            case "stored":
                return (Function<User, PrimaryGroupHolder>) PrimaryGroupHolder.Stored::new;
            case "parents-by-weight":
                return (Function<User, PrimaryGroupHolder>) PrimaryGroupHolder.ParentsByWeight::new;
            default:
                return (Function<User, PrimaryGroupHolder>) PrimaryGroupHolder.AllParentsByWeight::new;
        }
    }));

    /**
     * If set to false, the plugin will allow a Users primary group to be removed with the
     * 'parent remove' command, and will set their primary group back to default.
     */
    public static final ConfigKey<Boolean> PREVENT_PRIMARY_GROUP_REMOVAL = booleanKey("prevent-primary-group-removal", true);

    /**
     * If the plugin should check for "extra" permissions with users run LP commands
     */
    public static final ConfigKey<Boolean> USE_ARGUMENT_BASED_COMMAND_PERMISSIONS = booleanKey("argument-based-command-permissions", false);

    /**
     * If the plugin should check whether senders are a member of a given group
     * before they're able to edit the groups permissions or add/remove it from other users.
     */
    public static final ConfigKey<Boolean> REQUIRE_SENDER_GROUP_MEMBERSHIP_TO_MODIFY = booleanKey("require-sender-group-membership-to-modify", false);

    /**
     * If wildcards are being applied
     */
    public static final ConfigKey<Boolean> APPLYING_WILDCARDS = enduringKey(booleanKey("apply-wildcards", true));

    /**
     * If regex permissions are being applied
     */
    public static final ConfigKey<Boolean> APPLYING_REGEX = enduringKey(booleanKey("apply-regex", true));

    /**
     * If shorthand permissions are being applied
     */
    public static final ConfigKey<Boolean> APPLYING_SHORTHAND = enduringKey(booleanKey("apply-shorthand", true));

    /**
     * If Bukkit child permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_BUKKIT_CHILD_PERMISSIONS = enduringKey(booleanKey("apply-bukkit-child-permissions", true));

    /**
     * If Bukkit default permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_BUKKIT_DEFAULT_PERMISSIONS = enduringKey(booleanKey("apply-bukkit-default-permissions", true));

    /**
     * If Bukkit attachment permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_BUKKIT_ATTACHMENT_PERMISSIONS = enduringKey(booleanKey("apply-bukkit-attachment-permissions", true));

    /**
     * If Nukkit child permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_NUKKIT_CHILD_PERMISSIONS = enduringKey(booleanKey("apply-nukkit-child-permissions", true));

    /**
     * If Nukkit default permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_NUKKIT_DEFAULT_PERMISSIONS = enduringKey(booleanKey("apply-nukkit-default-permissions", true));

    /**
     * If Nukkit attachment permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_NUKKIT_ATTACHMENT_PERMISSIONS = enduringKey(booleanKey("apply-nukkit-attachment-permissions", true));

    /**
     * If BungeeCord configured permissions are being applied. This setting is ignored on other platforms.
     */
    public static final ConfigKey<Boolean> APPLY_BUNGEE_CONFIG_PERMISSIONS = enduringKey(booleanKey("apply-bungee-config-permissions", false));

    /**
     * If Sponge's implicit permission inheritance system should be applied
     */
    public static final ConfigKey<Boolean> APPLY_SPONGE_IMPLICIT_WILDCARDS = enduringKey(booleanKey("apply-sponge-implicit-wildcards", true));

    /**
     * If Sponge default subjects should be applied
     */
    public static final ConfigKey<Boolean> APPLY_SPONGE_DEFAULT_SUBJECTS = enduringKey(booleanKey("apply-sponge-default-subjects", true));

    /**
     * The algorithm LuckPerms should use when traversing the "inheritance tree"
     */
    public static final ConfigKey<TraversalAlgorithm> INHERITANCE_TRAVERSAL_ALGORITHM = customKey(c -> {
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
     * If a final sort according to "inheritance rules" should be performed after the traversal algorithm
     * has resolved the inheritance tree
     */
    public static final ConfigKey<Boolean> POST_TRAVERSAL_INHERITANCE_SORT = booleanKey("post-traversal-inheritance-sort", false);

    /**
     * The configured group weightings
     */
    public static final ConfigKey<Map<String, Integer>> GROUP_WEIGHTS = customKey(c -> {
        return c.getStringMap("group-weight", ImmutableMap.of()).entrySet().stream().collect(ImmutableCollectors.toMap(
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
    public static final ConfigKey<MetaStackDefinition> PREFIX_FORMATTING_OPTIONS = customKey(l -> {
        List<String> format = l.getStringList("meta-formatting.prefix.format", new ArrayList<>());
        if (format.isEmpty()) {
            format.add("highest");
        }
        String startSpacer = l.getString("meta-formatting.prefix.start-spacer", "");
        String middleSpacer = l.getString("meta-formatting.prefix.middle-spacer", " ");
        String endSpacer = l.getString("meta-formatting.prefix.end-spacer", "");
        DuplicateRemovalFunction duplicateRemovalFunction;
        switch (l.getString("meta-formatting.prefix.duplicates", "").toLowerCase()) {
            case "first-only":
                duplicateRemovalFunction = DuplicateRemovalFunction.FIRST_ONLY;
                break;
            case "last-only":
                duplicateRemovalFunction = DuplicateRemovalFunction.LAST_ONLY;
                break;
            default:
                duplicateRemovalFunction = DuplicateRemovalFunction.RETAIN_ALL;
                break;
        }

        return new SimpleMetaStackDefinition(StandardStackElements.parseList(l.getPlugin(), format), duplicateRemovalFunction, startSpacer, middleSpacer, endSpacer);
    });

    /**
     * Creates a new suffix MetaStack element based upon the configured values.
     */
    public static final ConfigKey<MetaStackDefinition> SUFFIX_FORMATTING_OPTIONS = customKey(l -> {
        List<String> format = l.getStringList("meta-formatting.suffix.format", new ArrayList<>());
        if (format.isEmpty()) {
            format.add("highest");
        }
        String startSpacer = l.getString("meta-formatting.suffix.start-spacer", "");
        String middleSpacer = l.getString("meta-formatting.suffix.middle-spacer", " ");
        String endSpacer = l.getString("meta-formatting.suffix.end-spacer", "");
        DuplicateRemovalFunction duplicateRemovalFunction;
        switch (l.getString("meta-formatting.prefix.duplicates", "").toLowerCase()) {
            case "first-only":
                duplicateRemovalFunction = DuplicateRemovalFunction.FIRST_ONLY;
                break;
            case "last-only":
                duplicateRemovalFunction = DuplicateRemovalFunction.LAST_ONLY;
                break;
            default:
                duplicateRemovalFunction = DuplicateRemovalFunction.RETAIN_ALL;
                break;
        }

        return new SimpleMetaStackDefinition(StandardStackElements.parseList(l.getPlugin(), format), duplicateRemovalFunction, startSpacer, middleSpacer, endSpacer);
    });

    /**
     * If log notifications are enabled
     */
    public static final ConfigKey<Boolean> LOG_NOTIFY = booleanKey("log-notify", true);

    /**
     * If auto op is enabled. Only used by the Bukkit platform.
     */
    public static final ConfigKey<Boolean> AUTO_OP = enduringKey(booleanKey("auto-op", false));

    /**
     * If server operators should be enabled. Only used by the Bukkit platform.
     */
    public static final ConfigKey<Boolean> OPS_ENABLED = enduringKey(customKey(c -> !AUTO_OP.get(c) && c.getBoolean("enable-ops", true)));

    /**
     * If server operators should be able to use LuckPerms commands by default. Only used by the Bukkit platform.
     */
    public static final ConfigKey<Boolean> COMMANDS_ALLOW_OP = enduringKey(booleanKey("commands-allow-op", true));

    /**
     * If Vault lookups for offline players on the main server thread should be enabled
     */
    public static final ConfigKey<Boolean> VAULT_UNSAFE_LOOKUPS = booleanKey("vault-unsafe-lookups", false);

    /**
     * Controls which group LuckPerms should use for NPC players when handling Vault requests
     */
    public static final ConfigKey<String> VAULT_NPC_GROUP = stringKey("vault-npc-group", "default");

    /**
     * Controls how LuckPerms should consider the OP status of NPC players when handing Vault requests
     */
    public static final ConfigKey<Boolean> VAULT_NPC_OP_STATUS = booleanKey("vault-npc-op-status", false);

    /**
     * If the vault server option should be used
     */
    public static final ConfigKey<Boolean> USE_VAULT_SERVER = booleanKey("use-vault-server", true);

    /**
     * The name of the server to use for Vault.
     */
    public static final ConfigKey<String> VAULT_SERVER = customKey(c -> {
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
    public static final ConfigKey<Boolean> VAULT_INCLUDING_GLOBAL = booleanKey("vault-include-global", true);

    /**
     * If any worlds provided with Vault lookups should be ignored
     */
    public static final ConfigKey<Boolean> VAULT_IGNORE_WORLD = booleanKey("vault-ignore-world", false);

    /**
     * The world rewrites map
     */
    public static final ConfigKey<Map<String, String>> WORLD_REWRITES = customKey(c -> {
        return c.getStringMap("world-rewrite", ImmutableMap.of()).entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> e.getKey().toLowerCase(),
                        e -> e.getValue().toLowerCase()
                ));
    });

    /**
     * The group name rewrites map
     */
    public static final ConfigKey<Map<String, String>> GROUP_NAME_REWRITES = mapKey("group-name-rewrite");

    /**
     * The database settings, username, password, etc for use by any database
     */
    public static final ConfigKey<StorageCredentials> DATABASE_VALUES = enduringKey(customKey(c -> {
        int maxPoolSize = c.getInteger("data.pool-settings.maximum-pool-size", c.getInteger("data.pool-size", 10));
        int minIdle = c.getInteger("data.pool-settings.minimum-idle", maxPoolSize);
        int maxLifetime = c.getInteger("data.pool-settings.maximum-lifetime", 1800000);
        int connectionTimeout = c.getInteger("data.pool-settings.connection-timeout", 5000);
        Map<String, String> props = ImmutableMap.copyOf(c.getStringMap("data.pool-settings.properties", ImmutableMap.of()));

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
    public static final ConfigKey<String> SQL_TABLE_PREFIX = enduringKey(stringKey("data.table_prefix", "luckperms_"));

    /**
     * The prefix for any MongoDB collections
     */
    public static final ConfigKey<String> MONGODB_COLLECTION_PREFIX = enduringKey(stringKey("data.mongodb_collection_prefix", ""));

    /**
     * MongoDB ClientConnectionURI to override default connection options
     */
    public static final ConfigKey<String> MONGODB_CONNECTION_URI = enduringKey(stringKey("data.mongodb_connection_URI", ""));

    /**
     * The name of the storage method being used
     */
    public static final ConfigKey<StorageType> STORAGE_METHOD = enduringKey(customKey(c -> {
        return StorageType.parse(c.getString("storage-method", "h2"), StorageType.H2);
    }));

    /**
     * If storage files should be monitored for changes
     */
    public static final ConfigKey<Boolean> WATCH_FILES = booleanKey("watch-files", true);

    /**
     * If split storage is being used
     */
    public static final ConfigKey<Boolean> SPLIT_STORAGE = enduringKey(booleanKey("split-storage.enabled", false));

    /**
     * The options for split storage
     */
    public static final ConfigKey<Map<SplitStorageType, StorageType>> SPLIT_STORAGE_OPTIONS = enduringKey(customKey(c -> {
        EnumMap<SplitStorageType, StorageType> map = new EnumMap<>(SplitStorageType.class);
        map.put(SplitStorageType.USER, StorageType.parse(c.getString("split-storage.methods.user", "h2"), StorageType.H2));
        map.put(SplitStorageType.GROUP, StorageType.parse(c.getString("split-storage.methods.group", "h2"), StorageType.H2));
        map.put(SplitStorageType.TRACK, StorageType.parse(c.getString("split-storage.methods.track", "h2"), StorageType.H2));
        map.put(SplitStorageType.UUID, StorageType.parse(c.getString("split-storage.methods.uuid", "h2"), StorageType.H2));
        map.put(SplitStorageType.LOG, StorageType.parse(c.getString("split-storage.methods.log", "h2"), StorageType.H2));
        return ImmutableMap.copyOf(map);
    }));

    /**
     * The name of the messaging service in use, or "none" if not enabled
     */
    public static final ConfigKey<String> MESSAGING_SERVICE = enduringKey(lowercaseStringKey("messaging-service", "auto"));

    /**
     * If updates should be automatically pushed by the messaging service
     */
    public static final ConfigKey<Boolean> AUTO_PUSH_UPDATES = enduringKey(booleanKey("auto-push-updates", true));

    /**
     * If LuckPerms should push logging entries to connected servers via the messaging service
     */
    public static final ConfigKey<Boolean> PUSH_LOG_ENTRIES = enduringKey(booleanKey("push-log-entries", true));

    /**
     * If LuckPerms should broadcast received logging entries to players on this platform
     */
    public static final ConfigKey<Boolean> BROADCAST_RECEIVED_LOG_ENTRIES = enduringKey(booleanKey("broadcast-received-log-entries", false));

    /**
     * If redis messaging is enabled
     */
    public static final ConfigKey<Boolean> REDIS_ENABLED = enduringKey(booleanKey("redis.enabled", false));

    /**
     * The address of the redis server
     */
    public static final ConfigKey<String> REDIS_ADDRESS = enduringKey(stringKey("redis.address", null));

    /**
     * The password in use by the redis server, or an empty string if there is no passworld
     */
    public static final ConfigKey<String> REDIS_PASSWORD = enduringKey(stringKey("redis.password", ""));

    /**
     * The URL of the bytebin instance used to upload data
     */
    public static final ConfigKey<String> BYTEBIN_URL = stringKey("bytebin-url", "https://bytebin.lucko.me/");

    /**
     * The URL of the web editor
     */
    public static final ConfigKey<String> WEB_EDITOR_URL_PATTERN = stringKey("web-editor-url", "https://editor.luckperms.net/");

    /**
     * The URL of the verbose viewer
     */
    public static final ConfigKey<String> VERBOSE_VIEWER_URL_PATTERN = stringKey("verbose-viewer-url", "https://luckperms.net/verbose/#");

    /**
     * The URL of the tree viewer
     */
    public static final ConfigKey<String> TREE_VIEWER_URL_PATTERN = stringKey("tree-viewer-url", "https://luckperms.net/treeview/#");

    private static final Map<String, ConfigKey<?>> KEYS;
    private static final int SIZE;

    static {
        Map<String, ConfigKey<?>> keys = new LinkedHashMap<>();
        Field[] values = ConfigKeys.class.getFields();
        int i = 0;

        for (Field f : values) {
            // ignore non-static fields
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }

            // ignore fields that aren't configkeys
            if (!ConfigKey.class.equals(f.getType())) {
                continue;
            }

            try {
                // get the key instance
                ConfigKeyTypes.BaseConfigKey<?> key = (ConfigKeyTypes.BaseConfigKey<?>) f.get(null);
                // set the ordinal value of the key.
                key.ordinal = i++;
                // add the key to the return map
                keys.put(f.getName(), key);
            } catch (Exception e) {
                throw new RuntimeException("Exception processing field: " + f, e);
            }
        }

        KEYS = ImmutableMap.copyOf(keys);
        SIZE = i;
    }

    /**
     * Gets a map of the keys defined in this class.
     *
     * <p>The string key in the map is the {@link Field#getName() field name}
     * corresponding to each key.</p>
     *
     * @return the defined keys
     */
    public static Map<String, ConfigKey<?>> getKeys() {
        return KEYS;
    }

    /**
     * Gets the number of defined keys.
     *
     * @return how many keys are defined in this class
     */
    public static int size() {
        return SIZE;
    }

}
