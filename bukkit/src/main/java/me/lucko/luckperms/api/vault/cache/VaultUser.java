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
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.vault.VaultPermissionHook;
import me.lucko.luckperms.users.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class VaultUser {
    private final LPBukkitPlugin plugin;
    private final VaultPermissionHook vault;

    @Getter
    private final User user;

    @Getter
    private final Map<Map<String, String>, ContextCache> contextData = new ConcurrentHashMap<>();

    @Getter
    private final Map<Map<String, String>, ChatCache> chatData = new ConcurrentHashMap<>();

    public boolean hasPermission(Map<String, String> context, String permission) {
        ContextCache cd = contextData.computeIfAbsent(context, map -> calculatePermissions(map, false));
        return cd.getPermissionValue(permission).asBoolean();
    }

    public ChatCache processChatData(Map<String, String> context) {
        return chatData.computeIfAbsent(context, map -> calculateChat(map, false));
    }

    public ContextCache calculatePermissions(Map<String, String> context, boolean apply) {
        Map<String, Boolean> toApply = user.exportNodes(
                new Contexts(context, vault.isIncludeGlobal(), true, true, true, true),
                Collections.emptyList(),
                true
        );

        ContextCache existing = contextData.get(context);
        if (existing == null) {
            existing = new ContextCache(user, context, plugin, plugin.getDefaultsProvider());
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

    public ChatCache calculateChat(Map<String, String> context, boolean apply) {
        ChatCache existing = chatData.get(context);
        if (existing == null) {
            existing = new ChatCache(context);
            if (apply) {
                chatData.put(context, existing);
            }
        }

        Map<String, String> contexts = new HashMap<>(context);
        String server = contexts.get("server");
        String world = contexts.get("world");
        contexts.remove("server");
        contexts.remove("world");

        existing.invalidateCache();

        // Load meta
        for (Node n : user.getPermissions(true)) {
            if (!n.getValue()) {
                continue;
            }

            if (!n.isMeta()) {
                continue;
            }

            if (!n.shouldApplyOnServer(server, vault.isIncludeGlobal(), false)) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, true, false)) {
                continue;
            }

            if (!n.shouldApplyWithContext(contexts, false)) {
                continue;
            }

            Map.Entry<String, String> meta = n.getMeta();
            existing.getMeta().put(meta.getKey(), meta.getValue());
        }

        // Load prefixes & suffixes

        int prefixPriority = Integer.MIN_VALUE;
        int suffixPriority = Integer.MIN_VALUE;

        for (Node n : user.getAllNodes(null, new Contexts(context, vault.isIncludeGlobal(), true, true, true, true))) {
            if (!n.getValue()) {
                continue;
            }

            if (!n.isPrefix() && !n.isSuffix()) {
                continue;
            }

            if (!n.shouldApplyOnServer(server, vault.isIncludeGlobal(), false)) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, true, false)) {
                continue;
            }

            if (!n.shouldApplyWithContext(contexts, false)) {
                continue;
            }

            if (n.isPrefix()) {
                Map.Entry<Integer, String> value = n.getPrefix();
                if (value.getKey() > prefixPriority) {
                    existing.setPrefix(value.getValue());
                }
            } else {
                Map.Entry<Integer, String> value = n.getSuffix();
                if (value.getKey() > suffixPriority) {
                    existing.setSuffix(value.getValue());
                }
            }
        }

        return existing;
    }
}
