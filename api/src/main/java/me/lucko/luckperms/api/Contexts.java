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

package me.lucko.luckperms.api;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;

/**
 * Represents the context and options for a permission lookup.
 * All values are immutable.
 * @since 2.11
 */
public class Contexts {
    public static final String SERVER_KEY = "server";
    public static final String WORLD_KEY = "world";

    /**
     * Gets a context that will allow all nodes
     * @return a context that will not apply any filters
     */
    public static Contexts allowAll() {
        return new Contexts(Collections.emptyMap(), true, true, true, true, true);
    }

    public static Contexts of(Map<String, String> context, boolean includeGlobal, boolean includeGlobalWorld, boolean applyGroups, boolean applyGlobalGroups, boolean applyGlobalWorldGroups) {
        return new Contexts(context, includeGlobal, includeGlobalWorld, applyGroups, applyGlobalGroups, applyGlobalWorldGroups);
    }

    public Contexts(Map<String, String> context, boolean includeGlobal, boolean includeGlobalWorld, boolean applyGroups, boolean applyGlobalGroups, boolean applyGlobalWorldGroups) {
        if (context == null) {
            throw new NullPointerException("context");
        }

        this.context = ImmutableMap.copyOf(context);
        this.includeGlobal = includeGlobal;
        this.includeGlobalWorld = includeGlobalWorld;
        this.applyGroups = applyGroups;
        this.applyGlobalGroups = applyGlobalGroups;
        this.applyGlobalWorldGroups = applyGlobalWorldGroups;
    }

    /**
     * The contexts that apply for this lookup
     *
     * The keys for servers and worlds are defined as static values.
     */
    private final Map<String, String> context;

    /**
     * If global or non server specific nodes should be applied
     */
    private final boolean includeGlobal;

    /**
     * If global or non world specific nodes should be applied
     */
    private final boolean includeGlobalWorld;

    /**
     * If parent groups should be applied
     */
    private final boolean applyGroups;

    /**
     * If global or non server specific group memberships should be applied
     */
    private final boolean applyGlobalGroups;

    /**
     * If global or non world specific group memberships should be applied
     */
    private final boolean applyGlobalWorldGroups;

    /**
     * Gets the contexts that apply for this lookup
     * @return an immutable map of context key value pairs
     */
    public Map<String, String> getContext() {
        return this.context;
    }

    /**
     * Gets if global or non server specific nodes should be applied
     * @return true if global or non server specific nodes should be applied
     */
    public boolean isIncludeGlobal() {
        return this.includeGlobal;
    }

    /**
     * Gets if global or non world specific nodes should be applied
     * @return true if global or non world specific nodes should be applied
     */
    public boolean isIncludeGlobalWorld() {
        return this.includeGlobalWorld;
    }

    /**
     * Gets if parent groups should be applied
     * @return true if parent groups should be applied
     */
    public boolean isApplyGroups() {
        return this.applyGroups;
    }

    /**
     * Gets if global or non server specific group memberships should be applied
     * @return true if global or non server specific group memberships should be applied
     */
    public boolean isApplyGlobalGroups() {
        return this.applyGlobalGroups;
    }

    /**
     * Gets if global or non world specific group memberships should be applied
     * @return true if global or non world specific group memberships should be applied
     */
    public boolean isApplyGlobalWorldGroups() {
        return this.applyGlobalWorldGroups;
    }

    public String toString() {
        return "Contexts(" +
                "context=" + this.getContext() + ", " +
                "includeGlobal=" + this.isIncludeGlobal() + ", " +
                "includeGlobalWorld=" + this.isIncludeGlobalWorld() + ", " +
                "applyGroups=" + this.isApplyGroups() + ", " +
                "applyGlobalGroups=" + this.isApplyGlobalGroups() + ", " +
                "applyGlobalWorldGroups=" + this.isApplyGlobalWorldGroups() +
                ")";
    }

}
