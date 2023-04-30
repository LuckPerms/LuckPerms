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

package me.lucko.luckperms.bukkit.util;

import com.google.common.graph.MutableGraph;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;

public final class PluginManagerUtil {
    private PluginManagerUtil() {}

    private static final Field DEPENDENCY_GRAPH_FIELD;

    static {
        Field dependencyGraphField = null;
        try {
            dependencyGraphField = SimplePluginManager.class.getDeclaredField("dependencyGraph");
            dependencyGraphField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
        DEPENDENCY_GRAPH_FIELD = dependencyGraphField;
    }

    /**
     * Injects a dependency relationship into the plugin manager.
     *
     * @param plugin the plugin
     * @param depend the plugin being depended on
     */
    @SuppressWarnings("unchecked")
    public static void injectDependency(PluginManager pluginManager, String plugin, String depend) {
        if (DEPENDENCY_GRAPH_FIELD == null || !(pluginManager instanceof SimplePluginManager)) {
            return; // fail silently
        }

        try {
            MutableGraph<String> graph = (MutableGraph<String>) DEPENDENCY_GRAPH_FIELD.get(pluginManager);
            graph.putEdge(plugin, depend);
        } catch (Exception e) {
            // ignore
        }
    }

}
