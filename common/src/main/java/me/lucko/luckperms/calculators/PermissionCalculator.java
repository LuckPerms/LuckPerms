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

package me.lucko.luckperms.calculators;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.constants.Patterns;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Calculates and caches permissions
 */
@RequiredArgsConstructor
public class PermissionCalculator {
    private final LuckPermsPlugin plugin;
    private final String objectName;
    private final boolean debug;
    private final List<PermissionProcessor> processors;
    private final Map<String, Tristate> cache = new ConcurrentHashMap<>();

    public void invalidateCache() {
        cache.clear();
    }

    public Tristate getPermissionValue(String permission) {
        permission = permission.toLowerCase();
        Tristate t =  cache.computeIfAbsent(permission, this::lookupPermissionValue);

        if (debug) {
            plugin.getLog().info("Checking if " + objectName + " has permission: " + permission + " - (" + t.toString() + ")");
        }

        return t;
    }

    private Tristate lookupPermissionValue(String permission) {
        for (PermissionProcessor processor : processors) {
            Tristate v = processor.hasPermission(permission);
            if (v == Tristate.UNDEFINED) {
                continue;
            }

            return v;
        }

        return Tristate.UNDEFINED;
    }

    @AllArgsConstructor
    public static class MapProcessor implements PermissionProcessor {

        @Getter
        private final Map<String, Boolean> map;

        @Override
        public Tristate hasPermission(String permission) {
            if (map.containsKey(permission)) {
                return Tristate.fromBoolean(map.get(permission));
            }

            return Tristate.UNDEFINED;
        }
    }

    @AllArgsConstructor
    public static class RegexProcessor implements PermissionProcessor {

        @Getter
        private final Map<String, Boolean> map;

        @Override
        public Tristate hasPermission(String permission) {
            for (Map.Entry<String, Boolean> e : map.entrySet()) {
                if (e.getKey().toLowerCase().startsWith("r=")) {
                    Pattern p = Patterns.compile(e.getKey().substring(2));
                    if (p == null) {
                        continue;
                    }

                    if (p.matcher(permission).matches()) {
                        return Tristate.fromBoolean(e.getValue());
                    }
                }
            }

            return Tristate.UNDEFINED;
        }
    }

    @AllArgsConstructor
    public static class WildcardProcessor implements PermissionProcessor {

        @Getter
        private final Map<String, Boolean> map;

        @Override
        public Tristate hasPermission(String permission) {
            String node = permission;

            while (node.contains(".")) {
                int endIndex = node.lastIndexOf('.');
                if (endIndex == -1) {
                    break;
                }

                node = node.substring(0, endIndex);
                if (!isEmpty(node)) {
                    if (map.containsKey(node + ".*")) {
                        return Tristate.fromBoolean(map.get(node + ".*"));
                    }
                }
            }

            if (map.containsKey("'*'")) {
                return Tristate.fromBoolean(map.get("'*'"));
            }

            if (map.containsKey("*")) {
                return Tristate.fromBoolean(map.get("*"));
            }

            return Tristate.UNDEFINED;
        }

        private static boolean isEmpty(String s) {
            if (s.equals("")) {
                return true;
            }

            char[] chars = s.toCharArray();
            for (char c : chars) {
                if (c != '.') {
                    return false;
                }
            }

            return true;
        }
    }
}
