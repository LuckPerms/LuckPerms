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

package me.lucko.luckperms.bukkit.calculator;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.calculator.processor.AbstractSourceBasedProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import net.luckperms.api.util.Tristate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Permission Processor for Bukkits "child" permission system.
 */
public class ChildProcessor extends AbstractSourceBasedProcessor implements PermissionProcessor {
    private static final TristateResult.Factory RESULT_FACTORY = new TristateResult.Factory(ChildProcessor.class);

    private final LPBukkitPlugin plugin;
    private final AtomicBoolean needsRefresh = new AtomicBoolean(false);
    private Map<String, TristateResult> childPermissions = Collections.emptyMap();

    public ChildProcessor(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TristateResult hasPermission(String permission) {
        if (this.needsRefresh.compareAndSet(true, false)) {
            refresh();
        }
        return this.childPermissions.getOrDefault(permission, TristateResult.UNDEFINED);
    }

    @Override
    public void refresh() {
        Map<String, TristateResult> childPermissions = new HashMap<>();
        this.sourceMap.forEach((key, node) -> {
            Map<String, Boolean> children = this.plugin.getPermissionMap().getChildPermissions(key, node.getValue());
            children.forEach((childKey, childValue) -> {
                childPermissions.put(childKey, RESULT_FACTORY.resultWithOverride(node, Tristate.of(childValue)));
            });
        });
        this.childPermissions = childPermissions;
        this.needsRefresh.set(false);
    }

    @Override
    public void invalidate() {
        this.needsRefresh.set(true);
    }
}
