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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.StaticContextCalculator;
import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class ApiContextManager implements me.lucko.luckperms.api.context.ContextManager {
    private final LuckPermsPlugin plugin;
    private final ContextManager handle;

    public ApiContextManager(LuckPermsPlugin plugin, ContextManager handle) {
        this.plugin = plugin;
        this.handle = handle;
    }

    private Object checkType(Object subject) {
        if (!this.handle.getSubjectClass().isAssignableFrom(subject.getClass())) {
            throw new IllegalStateException("Subject class " + subject.getClass() + " is not assignable from " + this.handle.getSubjectClass());
        }
        return subject;
    }

    @Override
    public @NonNull ImmutableContextSet getApplicableContext(@NonNull Object subject) {
        Objects.requireNonNull(subject, "subject");
        return this.handle.getApplicableContext(checkType(subject));
    }

    @Override
    public @NonNull Contexts getApplicableContexts(@NonNull Object subject) {
        Objects.requireNonNull(subject, "subject");
        return this.handle.getApplicableContexts(checkType(subject));
    }

    @Override
    public @NonNull Optional<ImmutableContextSet> lookupApplicableContext(@NonNull User user) {
        Objects.requireNonNull(user, "user");
        return this.plugin.getContextForUser(ApiUser.cast(user)).map(c -> c.getContexts().makeImmutable());
    }

    @Override
    public @NonNull Optional<Contexts> lookupApplicableContexts(@NonNull User user) {
        Objects.requireNonNull(user, "user");
        return this.plugin.getContextForUser(ApiUser.cast(user));
    }

    @Override
    public @NonNull ImmutableContextSet getStaticContext() {
        return this.handle.getStaticContext();
    }

    @Override
    public @NonNull Contexts getStaticContexts() {
        return this.handle.getStaticContexts();
    }

    @Override
    public @NonNull Contexts formContexts(@NonNull Object subject, @NonNull ImmutableContextSet contextSet) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(contextSet, "contextSet");
        return this.handle.formContexts(checkType(subject), contextSet);
    }

    @Override
    public @NonNull Contexts formContexts(@NonNull ImmutableContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        return this.handle.formContexts(contextSet);
    }

    @Override
    public void registerCalculator(@NonNull ContextCalculator<?> calculator) {
        Objects.requireNonNull(calculator, "calculator");
        this.handle.registerCalculator(calculator);
    }

    @Override
    public void registerStaticCalculator(@NonNull StaticContextCalculator calculator) {
        Objects.requireNonNull(calculator, "calculator");
        this.handle.registerStaticCalculator(calculator);
    }

    @Override
    public void invalidateCache(@NonNull Object subject) {
        Objects.requireNonNull(subject, "subject");
        this.handle.invalidateCache(checkType(subject));
    }
}
