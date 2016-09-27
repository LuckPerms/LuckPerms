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
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.api.vault.VaultPermissionHook;
import me.lucko.luckperms.contexts.Contexts;
import me.lucko.luckperms.users.User;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class VaultUserCache {
    private final LPBukkitPlugin plugin;
    private final VaultPermissionHook vault;

    @Getter
    private final User user;

    @Getter
    private final Map<Map<String, String>, ContextData> contextData = new ConcurrentHashMap<>();

    public boolean hasPermission(Map<String, String> context, String permission) {
        ContextData cd = contextData.computeIfAbsent(context, map -> calculatePermissions(map, false));
        return cd.getPermissionValue(permission).asBoolean();
    }

    public ContextData calculatePermissions(Map<String, String> context, boolean apply) {
        Map<String, Boolean> toApply = user.exportNodes(
                new Contexts(context, vault.isIncludeGlobal(), true, true, true, true),
                Collections.emptyList(),
                true
        );

        ContextData existing = contextData.get(context);
        if (existing == null) {
            existing = new ContextData(user, context, plugin, plugin.getDefaultsProvider());
            if (apply) {
                contextData.put(context, existing);
            }
        }

        boolean different = false;
        if (toApply.size() != existing.getPermissionCache().size()) {
            different = true;
        } else {
            for (Map.Entry<String, Boolean> e : existing.getPermissionCache().entrySet()) {
                if (toApply.containsKey(e.getKey()) && toApply.get(e.getKey()) == e.getValue()) {
                    continue;
                }
                different = true;
                break;
            }
        }

        if (!different) return existing;

        existing.getPermissionCache().clear();
        existing.invalidateCache();
        existing.getPermissionCache().putAll(toApply);
        return existing;
    }




}
