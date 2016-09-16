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

package me.lucko.luckperms.api.sponge;

import com.google.common.base.Splitter;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.users.User;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LuckPermsUserSubject extends LuckPermsSubject {
    public static LuckPermsUserSubject wrapUser(User user, LuckPermsService service) {
        return new LuckPermsUserSubject(user, service);
    }

    @Getter
    private final User user;

    @Getter
    private final Map<String, Boolean> permissionCache = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, Tristate> lookupCache = new HashMap<>();

    private LuckPermsUserSubject(User user, LuckPermsService service) {
        super(user, service);
        this.user = user;
    }

    public void invalidateCache() {
        synchronized (lookupCache) {
            lookupCache.clear();
        }
    }

    // TODO don't ignore context
    @Override
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String permission) {
        if (service.getPlugin().getConfiguration().getDebugPermissionChecks()) {
            service.getPlugin().getLog().info("Checking if " + user.getName() + " has permission: " + permission);
        }

        permission = permission.toLowerCase();
        synchronized (lookupCache) {
            if (lookupCache.containsKey(permission)) {
                return lookupCache.get(permission);
            } else {
                Tristate t = lookupPermissionValue(contexts, permission);
                lookupCache.put(permission, t);
                return t;
            }
        }
    }

    private Tristate lookupPermissionValue(Set<Context> contexts, String permission) {
        if (permissionCache.containsKey(permission)) {
            return Tristate.fromBoolean(permissionCache.get(permission));
        }

        if (service.getPlugin().getConfiguration().getApplyWildcards()) {
            if (permissionCache.containsKey("*")) {
                return Tristate.fromBoolean(permissionCache.get("*"));
            }
            if (permissionCache.containsKey("'*'")) {
                return Tristate.fromBoolean(permissionCache.get("'*'"));
            }

            String node = "";
            Iterable<String> permParts = Splitter.on('.').split(permission);
            for (String s : permParts) {
                if (node.equals("")) {
                    node = s;
                } else {
                    node = node + "." + s;
                }

                if (permissionCache.containsKey(node + ".*")) {
                    return Tristate.fromBoolean(permissionCache.get(node + ".*"));
                }
            }
        }


        return service.getDefaults().getPermissionValue(contexts, permission);
    }

    @Override
    public String getIdentifier() {
        return service.getPlugin().getUuidCache().getExternalUUID(user.getUuid()).toString();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        final UUID uuid = service.getPlugin().getUuidCache().getExternalUUID(user.getUuid());

        Optional<Player> p = Sponge.getServer().getPlayer(uuid);
        if (p.isPresent()) {
            return Optional.of(p.get());
        }

        return Optional.empty();
    }
}
