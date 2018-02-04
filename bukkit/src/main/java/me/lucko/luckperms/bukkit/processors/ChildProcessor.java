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

package me.lucko.luckperms.bukkit.processors;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.processors.AbstractPermissionProcessor;
import me.lucko.luckperms.common.processors.PermissionProcessor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permission Processor for Bukkits "child" permission system.
 */
public class ChildProcessor extends AbstractPermissionProcessor implements PermissionProcessor {
    private final LPBukkitPlugin plugin;
    private Map<String, Boolean> childPermissions = Collections.emptyMap();

    public ChildProcessor(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Tristate hasPermission(String permission) {
        return Tristate.fromNullableBoolean(this.childPermissions.get(permission));
    }

    @Override
    public void refresh() {
        Map<String, Boolean> builder = new ConcurrentHashMap<>();
        for (Map.Entry<String, Boolean> e : this.sourceMap.entrySet()) {
            Map<String, Boolean> children = this.plugin.getPermissionMap().getChildPermissions(e.getKey(), e.getValue());
            if (children != null) {
                builder.putAll(children);
            }
        }
        this.childPermissions = builder;
    }
}
