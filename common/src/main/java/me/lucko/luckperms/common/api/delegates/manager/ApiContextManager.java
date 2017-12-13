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

import lombok.AllArgsConstructor;
import lombok.NonNull;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ContextManager;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.StaticContextCalculator;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Optional;

@AllArgsConstructor
@SuppressWarnings("unchecked")
public class ApiContextManager implements ContextManager {
    private final LuckPermsPlugin plugin;
    private final me.lucko.luckperms.common.contexts.ContextManager handle;

    private Object checkType(Object subject) {
        if (!handle.getSubjectClass().isAssignableFrom(subject.getClass())) {
            throw new IllegalStateException("Subject class " + subject.getClass() + " is not assignable from " + handle.getSubjectClass());
        }
        return subject;
    }

    @Override
    public ImmutableContextSet getApplicableContext(@NonNull Object subject) {
        return handle.getApplicableContext(checkType(subject));
    }

    @Override
    public Contexts getApplicableContexts(@NonNull Object subject) {
        return handle.getApplicableContexts(checkType(subject));
    }

    @Override
    public Optional<ImmutableContextSet> lookupApplicableContext(@NonNull User user) {
        return Optional.ofNullable(plugin.getContextForUser(ApiUser.cast(user))).map(c -> c.getContexts().makeImmutable());
    }

    @Override
    public Optional<Contexts> lookupApplicableContexts(@NonNull User user) {
        return Optional.ofNullable(plugin.getContextForUser(ApiUser.cast(user)));
    }

    @Override
    public ImmutableContextSet getStaticContext() {
        return handle.getStaticContext();
    }

    @Override
    public Contexts getStaticContexts() {
        return handle.getStaticContexts();
    }

    @Override
    public Contexts formContexts(@NonNull Object subject, @NonNull ImmutableContextSet contextSet) {
        return handle.formContexts(checkType(subject), contextSet);
    }

    @Override
    public Contexts formContexts(@NonNull ImmutableContextSet contextSet) {
        return handle.formContexts(contextSet);
    }

    @Override
    public void registerCalculator(@NonNull ContextCalculator<?> calculator) {
        handle.registerCalculator(calculator);
    }

    @Override
    public void registerStaticCalculator(@NonNull StaticContextCalculator calculator) {
        handle.registerStaticCalculator(calculator);
    }

    @Override
    public void invalidateCache(@NonNull Object subject) {
        handle.invalidateCache(checkType(subject));
    }
}
