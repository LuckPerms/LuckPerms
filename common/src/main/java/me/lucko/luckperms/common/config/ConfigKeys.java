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

package me.lucko.luckperms.common.config;

import lombok.experimental.UtilityClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.common.config.keys.AbstractKey;
import me.lucko.luckperms.common.config.keys.BooleanKey;
import me.lucko.luckperms.common.config.keys.EnduringKey;
import me.lucko.luckperms.common.config.keys.IntegerKey;
import me.lucko.luckperms.common.config.keys.MapKey;
import me.lucko.luckperms.common.config.keys.StaticKey;
import me.lucko.luckperms.common.config.keys.StringKey;
import me.lucko.luckperms.common.defaults.Rule;
import me.lucko.luckperms.common.storage.DatastoreConfiguration;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.List;
import java.util.Map;

@UtilityClass
public class ConfigKeys {

    public static final ConfigKey<String> SERVER = StringKey.of("server", "global");
    public static final ConfigKey<Integer> SYNC_TIME = EnduringKey.wrap(IntegerKey.of("data.sync-minutes", 3));
    public static final ConfigKey<String> DEFAULT_GROUP_NODE = StaticKey.of("group.default"); // constant since 2.6
    public static final ConfigKey<String> DEFAULT_GROUP_NAME = StaticKey.of("default"); // constant since 2.6
    public static final ConfigKey<Boolean> INCLUDING_GLOBAL_PERMS = BooleanKey.of("include-global", true);
    public static final ConfigKey<Boolean> INCLUDING_GLOBAL_WORLD_PERMS = BooleanKey.of("include-global-world", true);
    public static final ConfigKey<Boolean> APPLYING_GLOBAL_GROUPS = BooleanKey.of("apply-global-groups", true);
    public static final ConfigKey<Boolean> APPLYING_GLOBAL_WORLD_GROUPS = BooleanKey.of("apply-global-world-groups", true);
    public static final ConfigKey<Boolean> ONLINE_MODE = BooleanKey.of("online-mode", true);
    public static final ConfigKey<Boolean> APPLYING_WILDCARDS = EnduringKey.wrap(BooleanKey.of("apply-wildcards", true));
    public static final ConfigKey<Boolean> APPLYING_REGEX = EnduringKey.wrap(BooleanKey.of("apply-regex", true));
    public static final ConfigKey<Boolean> APPLYING_SHORTHAND = EnduringKey.wrap(BooleanKey.of("apply-shorthand", true));
    public static final ConfigKey<Map<String, Integer>> GROUP_WEIGHTS = AbstractKey.of(c -> {
        return c.getMap("group-weight", ImmutableMap.of()).entrySet().stream().collect(ImmutableCollectors.toImmutableMap(
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
    public static final ConfigKey<Boolean> LOG_NOTIFY = BooleanKey.of("log-notify", true);
    public static final ConfigKey<Boolean> AUTO_OP = EnduringKey.wrap(BooleanKey.of("auto-op", false));
    public static final ConfigKey<Boolean> OPS_ENABLED = EnduringKey.wrap(AbstractKey.of(c -> !AUTO_OP.get(c) && c.getBoolean("enable-ops", true)));
    public static final ConfigKey<Boolean> COMMANDS_ALLOW_OP = EnduringKey.wrap(BooleanKey.of("commands-allow-op", true));
    public static final ConfigKey<String> VAULT_SERVER = StringKey.of("vault-server", "global");
    public static final ConfigKey<Boolean> VAULT_INCLUDING_GLOBAL = BooleanKey.of("vault-include-global", true);
    public static final ConfigKey<Boolean> VAULT_IGNORE_WORLD = BooleanKey.of("vault-ignore-world", false);
    public static final ConfigKey<Boolean> VAULT_PRIMARY_GROUP_OVERRIDES = BooleanKey.of("vault-primary-groups-overrides.enabled", false);
    public static final ConfigKey<Boolean> VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_INHERITED = BooleanKey.of("vault-primary-groups-overrides.check-inherited-permissions", false);
    public static final ConfigKey<Boolean> VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_EXISTS = BooleanKey.of("vault-primary-groups-overrides.check-group-exists", true);
    public static final ConfigKey<Boolean> VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_MEMBER_OF = BooleanKey.of("vault-primary-groups-overrides.check-user-member-of", true);
    public static final ConfigKey<Boolean> VAULT_DEBUG = BooleanKey.of("vault-debug", false);
    public static final ConfigKey<Map<String, String>> WORLD_REWRITES = MapKey.of("world-rewrite");
    public static final ConfigKey<Map<String, String>> GROUP_NAME_REWRITES = MapKey.of("group-name-rewrite");
    public static final ConfigKey<List<Rule>> DEFAULT_ASSIGNMENTS = AbstractKey.of(c -> {
        return c.getObjectList("default-assignments", ImmutableList.of()).stream().map(name -> {
            String hasTrue = c.getString("default-assignments." + name + ".if.has-true", null);
            String hasFalse = c.getString("default-assignments." + name + ".if.has-false", null);
            String lacks = c.getString("default-assignments." + name + ".if.lacks", null);
            List<String> give = ImmutableList.copyOf(c.getList("default-assignments." + name + ".give", ImmutableList.of()));
            List<String> take = ImmutableList.copyOf(c.getList("default-assignments." + name + ".take", ImmutableList.of()));
            String pg = c.getString("default-assignments." + name + ".set-primary-group", null);
            return new Rule(hasTrue, hasFalse, lacks, give, take, pg);
        }).collect(ImmutableCollectors.toImmutableList());
    });
    public static final ConfigKey<DatastoreConfiguration> DATABASE_VALUES = EnduringKey.wrap(AbstractKey.of(c -> {
        return new DatastoreConfiguration(
                c.getString("data.address", null),
                c.getString("data.database", null),
                c.getString("data.username", null),
                c.getString("data.password", null),
                c.getInt("data.pool-size", 10)
        );
    }));
    public static final ConfigKey<String> SQL_TABLE_PREFIX = EnduringKey.wrap(StringKey.of("data.table_prefix", "luckperms_"));
    public static final ConfigKey<String> STORAGE_METHOD = EnduringKey.wrap(StringKey.of("storage-method", "h2"));
    public static final ConfigKey<Boolean> SPLIT_STORAGE = EnduringKey.wrap(BooleanKey.of("split-storage.enabled", false));
    public static final ConfigKey<Map<String, String>> SPLIT_STORAGE_OPTIONS = EnduringKey.wrap(AbstractKey.of(c -> {
        return ImmutableMap.<String, String>builder()
                .put("user", c.getString("split-storage.methods.user", "h2"))
                .put("group", c.getString("split-storage.methods.group", "h2"))
                .put("track", c.getString("split-storage.methods.track", "h2"))
                .put("uuid", c.getString("split-storage.methods.uuid", "h2"))
                .put("log", c.getString("split-storage.methods.log", "h2"))
                .build();
    }));
    public static final ConfigKey<Boolean> REDIS_ENABLED = EnduringKey.wrap(BooleanKey.of("redis.enabled", false));
    public static final ConfigKey<String> REDIS_ADDRESS = EnduringKey.wrap(StringKey.of("redis.address", null));
    public static final ConfigKey<String> REDIS_PASSWORD = EnduringKey.wrap(StringKey.of("redis.password", ""));

    public static List<ConfigKey<?>> getAllKeys() {
        return ImmutableList.<ConfigKey<?>>builder()
                .add(SERVER)
                .add(SYNC_TIME)
                .add(DEFAULT_GROUP_NODE)
                .add(DEFAULT_GROUP_NAME)
                .add(INCLUDING_GLOBAL_PERMS)
                .add(INCLUDING_GLOBAL_WORLD_PERMS)
                .add(APPLYING_GLOBAL_GROUPS)
                .add(APPLYING_GLOBAL_WORLD_GROUPS)
                .add(ONLINE_MODE)
                .add(APPLYING_WILDCARDS)
                .add(APPLYING_REGEX)
                .add(APPLYING_SHORTHAND)
                .add(GROUP_WEIGHTS)
                .add(LOG_NOTIFY)
                .add(AUTO_OP)
                .add(OPS_ENABLED)
                .add(COMMANDS_ALLOW_OP)
                .add(VAULT_SERVER)
                .add(VAULT_INCLUDING_GLOBAL)
                .add(VAULT_IGNORE_WORLD)
                .add(VAULT_PRIMARY_GROUP_OVERRIDES)
                .add(VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_INHERITED)
                .add(VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_EXISTS)
                .add(VAULT_PRIMARY_GROUP_OVERRIDES_CHECK_MEMBER_OF)
                .add(VAULT_DEBUG)
                .add(WORLD_REWRITES)
                .add(GROUP_NAME_REWRITES)
                .add(DEFAULT_ASSIGNMENTS)
                .add(DATABASE_VALUES)
                .add(SQL_TABLE_PREFIX)
                .add(STORAGE_METHOD)
                .add(SPLIT_STORAGE)
                .add(SPLIT_STORAGE_OPTIONS)
                .add(REDIS_ENABLED)
                .add(REDIS_ADDRESS)
                .add(REDIS_PASSWORD)
                .build();
    }

}
