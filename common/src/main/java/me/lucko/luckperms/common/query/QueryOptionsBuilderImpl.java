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

import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.OptionKey;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class QueryOptionsBuilderImpl implements QueryOptions.Builder {
    private QueryMode mode;
    private ImmutableContextSet context;
    private final byte flags;
    private Set<Flag> flagsSet;
    private Map<OptionKey<?>, Object> options;
    private boolean copyOptions;

    public QueryOptionsBuilderImpl(QueryMode mode) {
        this.mode = mode;
        this.context = mode == QueryMode.CONTEXTUAL ? ImmutableContextSetImpl.EMPTY : null;
        this.flags = FlagUtils.ALL_FLAGS;
        this.flagsSet = null;
        this.options = null;
        this.copyOptions = false;
    }

    QueryOptionsBuilderImpl(QueryMode mode, ImmutableContextSet context, byte flags, Map<OptionKey<?>, Object> options) {
        this.mode = mode;
        this.context = context;
        this.flags = flags;
        this.flagsSet = null;
        this.options = options;
        this.copyOptions = true;
    }

    @Override
    public QueryOptions.@NonNull Builder mode(@NonNull QueryMode mode) {
        if (this.mode == mode) {
            return this;
        }

        this.mode = mode;
        this.context = this.mode == QueryMode.CONTEXTUAL ? ImmutableContextSetImpl.EMPTY : null;
        return this;
    }

    @Override
    public QueryOptions.@NonNull Builder context(@NonNull ContextSet context) {
        if (this.mode != QueryMode.CONTEXTUAL) {
            throw new IllegalStateException("Mode is not CONTEXTUAL");
        }

        this.context = Objects.requireNonNull(context, "context").immutableCopy();
        return this;
    }

    @Override
    public QueryOptions.@NonNull Builder flag(@NonNull Flag flag, boolean value) {
        Objects.requireNonNull(flag, "flag");

        // check if already set
        if (this.flagsSet == null && FlagUtils.read(this.flags, flag) == value) {
            return this;
        }

        if (this.flagsSet == null) {
            this.flagsSet = FlagUtils.toSet(this.flags);
        }
        if (value) {
            this.flagsSet.add(flag);
        } else {
            this.flagsSet.remove(flag);
        }

        return this;
    }

    @Override
    public QueryOptions.@NonNull Builder flags(@NonNull Set<Flag> flags) {
        Objects.requireNonNull(flags, "flags");
        this.flagsSet = EnumSet.copyOf(flags);
        return this;
    }

    @Override
    public <O> QueryOptions.@NonNull Builder option(@NonNull OptionKey<O> key, @Nullable O value) {
        Objects.requireNonNull(key, "key");

        if (this.options == null || this.copyOptions) {
            if (this.options != null) {
                this.options = new HashMap<>(this.options);
            } else {
                this.options = new HashMap<>();
            }
            this.copyOptions = false;
        }

        if (value == null) {
            this.options.remove(key);
        } else {
            this.options.put(key, value);
        }

        if (this.options.isEmpty()) {
            this.options = null;
        }

        return this;
    }

    @Override
    public @NonNull QueryOptions build() {
        byte flags = this.flagsSet != null ? FlagUtils.toByte(this.flagsSet) : this.flags;

        if (this.options == null) {
            if (this.mode == QueryMode.NON_CONTEXTUAL) {
                if (FlagUtils.ALL_FLAGS == flags) {
                    // mode same, contexts null, flags same, options null
                    // so therefore, equal to default - return that instead!
                    return QueryOptionsImpl.DEFAULT_NON_CONTEXTUAL;
                }
            } else if (this.mode == QueryMode.CONTEXTUAL) {
                if (FlagUtils.ALL_FLAGS == flags && this.context.isEmpty()) {
                    // mode same, contexts empty, flags same, options null
                    // so therefore, equal to default - return that instead!
                    return QueryOptionsImpl.DEFAULT_CONTEXTUAL;
                }
            }
        }

        return new QueryOptionsImpl(this.mode, this.context, flags, this.options);
    }
}
