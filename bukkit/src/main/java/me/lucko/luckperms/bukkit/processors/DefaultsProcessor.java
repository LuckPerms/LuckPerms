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

import lombok.AllArgsConstructor;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.processors.PermissionProcessor;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;

import java.util.Map;

/**
 * Permission Processor for Bukkits "default" permission system.
 */
@AllArgsConstructor
public class DefaultsProcessor implements PermissionProcessor {
    private final boolean isOp;
    private final DefaultsProvider defaultsProvider;

    @Override
    public Tristate hasPermission(String permission) {
        Tristate t = defaultsProvider.hasDefault(permission, isOp);
        if (t != Tristate.UNDEFINED) {
            return t;
        }

        Permission defPerm = Bukkit.getServer().getPluginManager().getPermission(permission);
        return defPerm == null ? Tristate.UNDEFINED : Tristate.fromBoolean(defPerm.getDefault().getValue(isOp));
    }

    @Override
    public void updateBacking(Map<String, Boolean> map) {
        // Do nothing, this doesn't use the backing
    }
}
