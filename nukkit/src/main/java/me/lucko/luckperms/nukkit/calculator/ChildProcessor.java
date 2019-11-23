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

package me.lucko.luckperms.nukkit.calculator;

import me.lucko.luckperms.common.calculator.processor.AbstractPermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.calculator.result.TristateResult;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;

import net.luckperms.api.util.Tristate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permission Processor for Nukkits "child" permission system.
 */
public class ChildProcessor extends AbstractPermissionProcessor implements PermissionProcessor {
    private static final TristateResult.Factory RESULT_FACTORY = new TristateResult.Factory(ChildProcessor.class);

    private final LPNukkitPlugin plugin;
    private Map<String, TristateResult> childPermissions = Collections.emptyMap();

    public ChildProcessor(LPNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TristateResult hasPermission(String permission) {
        return this.childPermissions.getOrDefault(permission, TristateResult.UNDEFINED);
    }

    @Override
    public void refresh() {
        Map<String, TristateResult> builder = new ConcurrentHashMap<>();
        for (Map.Entry<String, Boolean> e : this.sourceMap.entrySet()) {
            Map<String, Boolean> children = this.plugin.getPermissionMap().getChildPermissions(e.getKey(), e.getValue());
            for (Map.Entry<String, Boolean> child : children.entrySet()) {
                builder.put(child.getKey(), RESULT_FACTORY.result(Tristate.of(child.getValue()), "parent: " + e.getKey()));
            }
        }
        this.childPermissions = builder;
    }

    @Override
    public void invalidate() {
        refresh();
    }
}
