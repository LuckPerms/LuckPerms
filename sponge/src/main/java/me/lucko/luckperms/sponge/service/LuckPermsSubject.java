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

package me.lucko.luckperms.sponge.service;

import lombok.NonNull;
import me.lucko.luckperms.api.context.ContextSet;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class LuckPermsSubject implements Subject {

    protected abstract Tristate getPermissionValue(ContextSet contexts, String permission);
    protected abstract boolean isChildOf(ContextSet contexts, Subject parent);
    protected abstract List<Subject> getParents(ContextSet contexts);
    protected abstract Optional<String> getOption(ContextSet contexts, String s);
    protected abstract ContextSet getActiveContextSet();

    @Override
    @Deprecated
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String permission) {
        return getPermissionValue(LuckPermsService.convertContexts(contexts), permission);
    }

    @Override
    @Deprecated
    public boolean hasPermission(@NonNull Set<Context> contexts, @NonNull String permission) {
        return getPermissionValue(LuckPermsService.convertContexts(contexts), permission).asBoolean();
    }

    @Override
    @Deprecated
    public boolean hasPermission(@NonNull String permission) {
        return getPermissionValue(getActiveContextSet(), permission).asBoolean();
    }

    @Override
    @Deprecated
    public boolean isChildOf(@NonNull Set<Context> contexts, @NonNull Subject parent) {
        return isChildOf(LuckPermsService.convertContexts(contexts), parent);
    }

    @Override
    @Deprecated
    public boolean isChildOf(@NonNull Subject parent) {
        return isChildOf(getActiveContextSet(), parent);
    }

    @Override
    @Deprecated
    public List<Subject> getParents(@NonNull Set<Context> contexts) {
        return getParents(LuckPermsService.convertContexts(contexts));
    }

    @Override
    @Deprecated
    public List<Subject> getParents() {
        return getParents(getActiveContextSet());
    }

    @Override
    @Deprecated
    public Optional<String> getOption(@NonNull Set<Context> contexts, @NonNull String s) {
        return getOption(LuckPermsService.convertContexts(contexts), s);
    }

    @Override
    @Deprecated
    public Optional<String> getOption(@NonNull String key) {
        return getOption(getActiveContextSet(), key);
    }

    @Override
    @Deprecated
    public Set<Context> getActiveContexts() {
        return LuckPermsService.convertContexts(getActiveContextSet());
    }

}
