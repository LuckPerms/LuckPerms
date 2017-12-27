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

package me.lucko.luckperms.api;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Encapsulates the options and settings for a permission or meta lookup.
 *
 * <p>This class is immutable.</p>
 *
 * @since 2.11
 */
@Immutable
public class Contexts {

    /**
     * The context key used to denote the subjects server
     */
    public static final String SERVER_KEY = "server";

    /**
     * The context key used to denote the subjects world
     */
    public static final String WORLD_KEY = "world";

    /**
     * A 'global' or default contexts instance.
     *
     * Simply passes an empty context set, with all accumulation settings set to true.
     */
    private static final Contexts GLOBAL = new Contexts(
            ContextSet.empty(),
            true,
            true,
            true,
            true,
            true,
            false
    );

    /**
     * Gets the {@link FullySatisfiedContexts} instance.
     *
     * @return a context that will satisfy all contextual requirements.
     */
    @Nonnull
    public static Contexts allowAll() {
        return FullySatisfiedContexts.getInstance();
    }

    /**
     * A contexts instance with no defined context.
     *
     * @return the global contexts
     * @since 3.3
     */
    @Nonnull
    public static Contexts global() {
        return GLOBAL;
    }

    public static Contexts of(@Nonnull ContextSet context, boolean includeGlobal, boolean includeGlobalWorld, boolean applyGroups, boolean applyGlobalGroups, boolean applyGlobalWorldGroups, boolean op) {
        return new Contexts(context, includeGlobal, includeGlobalWorld, applyGroups, applyGlobalGroups, applyGlobalWorldGroups, op);
    }

    /**
     * The contexts that apply for this lookup
     */
    private final ImmutableContextSet context;

    /**
     * If the target subject is OP. This is used to parse defaults on Bukkit,
     * and is ignored on all other platforms.
     *
     * @since 2.12
     */
    private final boolean op;

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

    // cache hashcode - this class is immutable, and is used as an index in the permission cache.
    private final int hashCode;

    public Contexts(@Nonnull ContextSet context, boolean includeGlobal, boolean includeGlobalWorld, boolean applyGroups, boolean applyGlobalGroups, boolean applyGlobalWorldGroups, boolean op) {
        this.context = Preconditions.checkNotNull(context, "context").makeImmutable();
        this.includeGlobal = includeGlobal;
        this.includeGlobalWorld = includeGlobalWorld;
        this.applyGroups = applyGroups;
        this.applyGlobalGroups = applyGlobalGroups;
        this.applyGlobalWorldGroups = applyGlobalWorldGroups;
        this.op = op;
        this.hashCode = calculateHashCode();
    }

    /**
     * Gets the contexts that apply for this lookup
     *
     * @return an immutable set of context key value pairs
     * @since 2.13
     */
    @Nonnull
    public ContextSet getContexts() {
        return this.context;
    }

    /**
     * Gets if the target subject is OP. This is used to parse defaults on Bukkit,
     * and is ignored on all other platforms.
     *
     * @return true if op defaults should be included
     */
    public boolean isOp() {
        return this.op;
    }

    /**
     * Gets if global or non server specific nodes should be applied
     *
     * @return true if global or non server specific nodes should be applied
     */
    public boolean isIncludeGlobal() {
        return this.includeGlobal;
    }

    /**
     * Gets if global or non world specific nodes should be applied
     *
     * @return true if global or non world specific nodes should be applied
     */
    public boolean isIncludeGlobalWorld() {
        return this.includeGlobalWorld;
    }

    /**
     * Gets if parent groups should be applied
     *
     * @return true if parent groups should be applied
     */
    public boolean isApplyGroups() {
        return this.applyGroups;
    }

    /**
     * Gets if global or non server specific group memberships should be applied
     *
     * @return true if global or non server specific group memberships should be applied
     */
    public boolean isApplyGlobalGroups() {
        return this.applyGlobalGroups;
    }

    /**
     * Gets if global or non world specific group memberships should be applied
     *
     * @return true if global or non world specific group memberships should be applied
     */
    public boolean isApplyGlobalWorldGroups() {
        return this.applyGlobalWorldGroups;
    }

    @Override
    @Nonnull
    public String toString() {
        return "Contexts(" +
                "context=" + this.getContexts() + ", " +
                "op=" + this.isOp() + ", " +
                "includeGlobal=" + this.isIncludeGlobal() + ", " +
                "includeGlobalWorld=" + this.isIncludeGlobalWorld() + ", " +
                "applyGroups=" + this.isApplyGroups() + ", " +
                "applyGlobalGroups=" + this.isApplyGlobalGroups() + ", " +
                "applyGlobalWorldGroups=" + this.isApplyGlobalWorldGroups() +
                ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == allowAll()) return false;
        if (!(o instanceof Contexts)) return false;

        final Contexts other = (Contexts) o;
        return this.getContexts().equals(other.getContexts()) &&
                this.isOp() == other.isOp() &&
                this.isIncludeGlobal() == other.isIncludeGlobal() &&
                this.isIncludeGlobalWorld() == other.isIncludeGlobalWorld() &&
                this.isApplyGroups() == other.isApplyGroups() &&
                this.isApplyGlobalGroups() == other.isApplyGlobalGroups() &&
                this.isApplyGlobalWorldGroups() == other.isApplyGlobalWorldGroups();
    }

    private int calculateHashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object contexts = this.getContexts();
        result = result * PRIME + contexts.hashCode();
        result = result * PRIME + (this.isOp() ? 79 : 97);
        result = result * PRIME + (this.isIncludeGlobal() ? 79 : 97);
        result = result * PRIME + (this.isIncludeGlobalWorld() ? 79 : 97);
        result = result * PRIME + (this.isApplyGroups() ? 79 : 97);
        result = result * PRIME + (this.isApplyGlobalGroups() ? 79 : 97);
        result = result * PRIME + (this.isApplyGlobalWorldGroups() ? 79 : 97);
        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
