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
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LuckPermsUserSubject extends LuckPermsSubject {
    public static LuckPermsUserSubject wrapUser(User user, LuckPermsService service) {
        return new LuckPermsUserSubject(user, service);
    }

    @Getter
    private final User user;

    @Getter
    private final Map<String, Boolean> permissionCache = new ConcurrentHashMap<>();

    private LuckPermsUserSubject(User user, LuckPermsService service) {
        super(user, service);
        this.user = user;
    }

    // TODO don't ignore context
    @Override
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String permission) {
        if (service.getPlugin().getConfiguration().getDebugPermissionChecks()) {
            service.getPlugin().getLog().info("Checking if " + user.getName() + " has permission: " + permission);
        }

        permission = permission.toLowerCase();

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

        return Tristate.UNDEFINED;
    }
}
