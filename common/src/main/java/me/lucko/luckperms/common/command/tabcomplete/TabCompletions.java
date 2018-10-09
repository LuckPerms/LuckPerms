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

package me.lucko.luckperms.common.command.tabcomplete;

import com.google.common.base.Splitter;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.treeview.PermissionRegistry;
import me.lucko.luckperms.common.treeview.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Common completion suppliers used by the plugin
 */
public final class TabCompletions {

    private static final CompletionSupplier BOOLEAN = CompletionSupplier.startsWith("true", "false");

    private final CompletionSupplier groups;
    private final CompletionSupplier tracks;
    private final CompletionSupplier permissions;

    public TabCompletions(LuckPermsPlugin plugin) {
        this.groups = CompletionSupplier.startsWith(() -> plugin.getGroupManager().getAll().keySet());
        this.tracks = CompletionSupplier.startsWith(() -> plugin.getTrackManager().getAll().keySet());
        this.permissions = partial -> {
            PermissionRegistry cache = plugin.getPermissionRegistry();

            if (partial.isEmpty()) {
                return cache.getRootNode().getChildren()
                        .map(Map::keySet)
                        .<List<String>>map(ArrayList::new)
                        .orElse(Collections.emptyList());
            }

            String start = partial.toLowerCase();
            List<String> parts = new ArrayList<>(Splitter.on('.').splitToList(start));
            TreeNode root = cache.getRootNode();

            if (parts.size() <= 1) {
                if (!root.getChildren().isPresent()) {
                    return Collections.emptyList();
                }

                return root.getChildren().get().keySet().stream().filter(TabCompleter.startsWithIgnoreCase(start)).collect(Collectors.toList());
            }

            String incomplete = parts.remove(parts.size() - 1);

            for (String s : parts) {
                if (!root.getChildren().isPresent()) {
                    return Collections.emptyList();
                }

                TreeNode n = root.getChildren().get().get(s);
                if (n == null) {
                    return Collections.emptyList();
                }

                root = n;
            }

            if (!root.getChildren().isPresent()) {
                return Collections.emptyList();
            }

            return root.getChildren().get().keySet().stream()
                    .filter(TabCompleter.startsWithIgnoreCase(incomplete))
                    .map(s -> String.join(".", parts) + "." + s)
                    .collect(Collectors.toList());
        };
    }

    // bit of a weird pattern, but meh it kinda works, reduces the boilerplate
    // of calling the commandmanager + tabcompletions getters every time

    public static CompletionSupplier booleans() {
        return BOOLEAN;
    }

    public static CompletionSupplier groups(LuckPermsPlugin plugin) {
        return plugin.getCommandManager().getTabCompletions().groups;
    }

    public static CompletionSupplier tracks(LuckPermsPlugin plugin) {
        return plugin.getCommandManager().getTabCompletions().tracks;
    }

    public static CompletionSupplier permissions(LuckPermsPlugin plugin) {
        return plugin.getCommandManager().getTabCompletions().permissions;
    }

}
