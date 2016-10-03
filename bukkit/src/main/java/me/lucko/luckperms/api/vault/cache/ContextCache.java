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

package me.lucko.luckperms.api.vault.cache;

import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.calculators.*;
import me.lucko.luckperms.users.BukkitUser;
import me.lucko.luckperms.users.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContextCache {

    @Getter
    private final Map<String, String> context;

    @Getter
    private final Map<String, Boolean> permissionCache = new ConcurrentHashMap<>();

    private final PermissionCalculator calculator;

    public ContextCache(User user, Map<String, String> context, LuckPermsPlugin plugin, DefaultsProvider defaultsProvider) {
        this.context = context;

        List<PermissionProcessor> processors = new ArrayList<>(5);
        processors.add(new MapProcessor(permissionCache));
        if (plugin.getConfiguration().isApplyingWildcards()) {
            processors.add(new WildcardProcessor(permissionCache));
        }
        if (plugin.getConfiguration().isApplyingRegex()) {
            processors.add(new RegexProcessor(permissionCache));
        }

        processors.add(new DefaultsProcessor(() -> ((BukkitUser) user).getLpPermissible().isOp(), defaultsProvider));
        calculator = new PermissionCalculator(plugin, user.getName(), plugin.getConfiguration().isDebugPermissionChecks(), processors);
    }

    public void invalidateCache() {
        calculator.invalidateCache();
    }

    public Tristate getPermissionValue(@NonNull String permission) {
        return calculator.getPermissionValue(permission);
    }

}
