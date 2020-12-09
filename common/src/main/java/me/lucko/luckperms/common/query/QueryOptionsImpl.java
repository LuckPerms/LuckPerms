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

package me.lucko.luckperms.common.query;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;

import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.OptionKey;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class QueryOptionsImpl implements QueryOptions {
    public static final QueryOptions DEFAULT_CONTEXTUAL = new QueryOptionsImpl(QueryMode.CONTEXTUAL, ImmutableContextSetImpl.EMPTY, FlagUtils.ALL_FLAGS, null);
    public static final QueryOptions DEFAULT_NON_CONTEXTUAL = new QueryOptionsImpl(QueryMode.NON_CONTEXTUAL, null, FlagUtils.ALL_FLAGS, null);

    // state
    private final QueryMode mode;
    private final ImmutableContextSet context;
    private final byte flags;
    private final ImmutableMap<OptionKey<?>, Object> options;

    // computed based on state above
    private final int hashCode;
    private Set<Flag> flagsSet = null;
    private final ContextSatisfyMode contextSatisfyMode;

    QueryOptionsImpl(QueryMode mode, @Nullable ImmutableContextSet context, byte flags, @Nullable Map<OptionKey<?>, Object> options) {
        this.mode = mode;
        this.context = context;
        this.flags = flags;
        this.options = options == null ? null : ImmutableMap.copyOf(options);

        this.hashCode = calculateHashCode();
        this.contextSatisfyMode = options == null ? null : (ContextSatisfyMode) options.get(ContextSatisfyMode.KEY);
    }

    @Override
    public @NonNull QueryMode mode() {
        return this.mode;
    }

    @Override
    public @NonNull ImmutableContextSet context() {
        if (this.mode != QueryMode.CONTEXTUAL) {
            throw new IllegalStateException("Mode is not CONTEXTUAL");
        }
        return this.context;
    }

    @Override
    public boolean flag(@NonNull Flag flag) {
        Objects.requireNonNull(flag, "flag");
        return FlagUtils.read(this.flags, flag);
    }

    @Override
    public @NonNull Set<Flag> flags() {
        if (this.flagsSet != null) {
            return this.flagsSet;
        }
        Set<Flag> set = ImmutableSet.copyOf(FlagUtils.toSet(this.flags));
        this.flagsSet = set;
        return set;
    }

    @Override
    public @NonNull <O> Optional<O> option(@NonNull OptionKey<O> key) {
        if (this.options == null) {
            return Optional.empty();
        }

        //noinspection unchecked
        return Optional.ofNullable((O) this.options.get(key));
    }

    @Override
    public @NonNull Map<OptionKey<?>, Object> options() {
        if (this.options == null) {
            return ImmutableMap.of();
        }
        return this.options;
    }

    @Override
    public boolean satisfies(@NonNull ContextSet contextSet, @NonNull ContextSatisfyMode defaultContextSatisfyMode) {
        switch (this.mode) {
            case CONTEXTUAL:
                return contextSet.isSatisfiedBy(this.context, this.contextSatisfyMode == null ? defaultContextSatisfyMode : this.contextSatisfyMode);
            case NON_CONTEXTUAL:
                return true;
            default:
                throw new AssertionError();
        }
    }

    @Override
    public @NonNull Builder toBuilder() {
        return new QueryOptionsBuilderImpl(this.mode, this.context, this.flags, this.options);
    }

    @Override
    public String toString() {
        return "QueryOptions(mode=" + this.mode + ", context=" + this.context + ", flags=" + flags() + ", options=" + this.options + ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryOptionsImpl that = (QueryOptionsImpl) o;
        return this.flags == that.flags &&
                this.mode == that.mode &&
                Objects.equals(this.context, that.context) &&
                Objects.equals(this.options, that.options);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    private int calculateHashCode() {
        int result = this.mode.hashCode();
        result = 31 * result + (this.context != null ? this.context.hashCode() : 0);
        result = 31 * result + (int) this.flags;
        result = 31 * result + (this.options != null ? this.options.hashCode() : 0);
        return result;
    }
}
