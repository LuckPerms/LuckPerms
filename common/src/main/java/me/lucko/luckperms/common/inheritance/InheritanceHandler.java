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

package me.lucko.luckperms.common.inheritance;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.concurrent.TimeUnit;

/**
 * Provides {@link InheritanceGraph}s.
 */
public class InheritanceHandler {
    private final LuckPermsPlugin plugin;

    /**
     * An inheritance graph which doesn't consider contexts
     */
    private final InheritanceGraph nonContextualGraph;

    /**
     * Cache of contextual inheritance graph instances
     */
    private final LoadingCache<Contexts, InheritanceGraph> contextualGraphs;

    public InheritanceHandler(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.nonContextualGraph = new InheritanceGraph.NonContextual(plugin);
        this.contextualGraphs = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(key -> new InheritanceGraph.Contextual(this.plugin, key));
    }

    public InheritanceGraph getGraph() {
        return this.nonContextualGraph;
    }

    public InheritanceGraph getGraph(Contexts contexts) {
        return this.contextualGraphs.get(contexts);
    }

}
