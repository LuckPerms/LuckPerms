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

package me.lucko.luckperms.common.api.delegates.manager;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.StaticContextCalculator;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

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

    @Nonnull
    @Override
    public ImmutableContextSet getApplicableContext(@Nonnull Object subject) {
        Objects.requireNonNull(subject, "subject");
        return this.handle.getApplicableContext(checkType(subject));
    }

    @Nonnull
    @Override
    public Contexts getApplicableContexts(@Nonnull Object subject) {
        Objects.requireNonNull(subject, "subject");
        return this.handle.getApplicableContexts(checkType(subject));
    }

    @Nonnull
    @Override
    public Optional<ImmutableContextSet> lookupApplicableContext(@Nonnull User user) {
        Objects.requireNonNull(user, "user");
        return this.plugin.getContextForUser(ApiUser.cast(user)).map(c -> c.getContexts().makeImmutable());
    }

    @Nonnull
    @Override
    public Optional<Contexts> lookupApplicableContexts(@Nonnull User user) {
        Objects.requireNonNull(user, "user");
        return this.plugin.getContextForUser(ApiUser.cast(user));
    }

    @Nonnull
    @Override
    public ImmutableContextSet getStaticContext() {
        return this.handle.getStaticContext();
    }

    @Nonnull
    @Override
    public Contexts getStaticContexts() {
        return this.handle.getStaticContexts();
    }

    @Nonnull
    @Override
    public Contexts formContexts(@Nonnull Object subject, @Nonnull ImmutableContextSet contextSet) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(contextSet, "contextSet");
        return this.handle.formContexts(checkType(subject), contextSet);
    }

    @Nonnull
    @Override
    public Contexts formContexts(@Nonnull ImmutableContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        return this.handle.formContexts(contextSet);
    }

    @Override
    public void registerCalculator(@Nonnull ContextCalculator<?> calculator) {
        Objects.requireNonNull(calculator, "calculator");
        this.handle.registerCalculator(calculator);
    }

    @Override
    public void registerStaticCalculator(@Nonnull StaticContextCalculator calculator) {
        Objects.requireNonNull(calculator, "calculator");
        this.handle.registerStaticCalculator(calculator);
    }

    @Override
    public void invalidateCache(@Nonnull Object subject) {
        Objects.requireNonNull(subject, "subject");
        this.handle.invalidateCache(checkType(subject));
    }
}
