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

package me.lucko.luckperms.common.command.utils;

import com.google.common.base.Splitter;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.treeview.PermissionRegistry;
import me.lucko.luckperms.common.treeview.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility methods for handling tab completion.
 */
public final class TabCompletions {

    public static List<String> getGroupTabComplete(List<String> args, LuckPermsPlugin plugin) {
        return getTabComplete(new ArrayList<>(plugin.getGroupManager().getAll().keySet()), args);
    }

    public static List<String> getTrackTabComplete(List<String> args, LuckPermsPlugin plugin) {
        return getTabComplete(new ArrayList<>(plugin.getTrackManager().getAll().keySet()), args);
    }

    public static List<String> getBoolTabComplete(List<String> args) {
        if (args.size() == 2) {
            return Arrays.asList("true", "false");
        } else {
            return Collections.emptyList();
        }
    }

    public static List<String> getPermissionTabComplete(List<String> args, PermissionRegistry cache) {
        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equals("")) {
                return cache.getRootNode().getChildren()
                        .map(Map::keySet)
                        .map(s -> (List<String>) new ArrayList<>(s))
                        .orElse(Collections.emptyList());
            }

            String start = args.get(0).toLowerCase();
            List<String> parts = new ArrayList<>(Splitter.on('.').splitToList(start));
            TreeNode root = cache.getRootNode();

            if (parts.size() <= 1) {
                if (!root.getChildren().isPresent()) {
                    return Collections.emptyList();
                }

                return root.getChildren().get().keySet().stream().filter(s -> s.startsWith(start)).collect(Collectors.toList());
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
                    .filter(s -> s.startsWith(incomplete))
                    .map(s -> parts.stream().collect(Collectors.joining(".")) + "." + s)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private static List<String> getTabComplete(List<String> options, List<String> args) {
        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return options;
            }

            return options.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private TabCompletions() {}
}
