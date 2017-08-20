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

package me.lucko.luckperms.sponge.service.persisted;

import lombok.Getter;
import lombok.Setter;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Extension of MemorySubjectData which persists data when modified
 */
public class PersistedSubjectData extends CalculatedSubjectData implements Function<Boolean, Boolean> {
    private final PersistedSubject subject;

    @Getter
    @Setter
    private boolean save = true;

    public PersistedSubjectData(LuckPermsService service, String calculatorDisplayName, PersistedSubject subject) {
        super(subject, service, calculatorDisplayName);
        this.subject = subject;
    }

    private void save() {
        if (!save) {
            return;
        }

        if (subject != null) {
            subject.save();
        }
    }

    @Override
    public Boolean apply(Boolean b) {
        save();
        return b;
    }

    @Override
    public CompletableFuture<Boolean> setPermission(ImmutableContextSet contexts, String permission, Tristate value) {
        return super.setPermission(contexts, permission, value).thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        return super.clearPermissions().thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(ImmutableContextSet contexts) {
        return super.clearPermissions(contexts).thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> addParent(ImmutableContextSet contexts, SubjectReference parent) {
        return super.addParent(contexts, parent).thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> removeParent(ImmutableContextSet contexts, SubjectReference parent) {
        return super.removeParent(contexts, parent).thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        return super.clearParents().thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> clearParents(ImmutableContextSet contexts) {
        return super.clearParents(contexts).thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> setOption(ImmutableContextSet contexts, String key, String value) {
        return super.setOption(contexts, key, value).thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> unsetOption(ImmutableContextSet contexts, String key) {
        return super.unsetOption(contexts, key).thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        return super.clearOptions().thenApply(this);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(ImmutableContextSet contexts) {
        return super.clearOptions(contexts).thenApply(this);
    }
}
