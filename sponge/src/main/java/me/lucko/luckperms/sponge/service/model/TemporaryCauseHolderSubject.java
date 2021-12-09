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

package me.lucko.luckperms.sponge.service.model;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TemporaryCauseHolderSubject implements Subject {
    private final @NonNull Cause cause;
    private final @Nullable Subject subject;

    public TemporaryCauseHolderSubject(@NonNull Cause cause) {
        this.cause = cause;
        this.subject = subjectFromCause(this.cause);
    }

    private static Subject subjectFromCause(Cause cause) {
        Subject subject = cause.context().get(EventContextKeys.SUBJECT).orElse(null);
        if (subject != null) {
            return subject;
        }

        return cause.first(Subject.class).orElse(null);
    }

    public @NonNull Cause getCause() {
        return this.cause;
    }

    public @Nullable Subject getSubject() {
        return this.subject;
    }

    @Override
    public String toString() {
        return "CauseSubject(cause=" + this.cause + ')';
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof TemporaryCauseHolderSubject && this.cause.equals(((TemporaryCauseHolderSubject) o).cause));
    }

    @Override
    public int hashCode() {
        return this.cause.hashCode();
    }

    @Override public SubjectCollection containingCollection() { throw new UnsupportedOperationException(); }
    @Override public SubjectReference asSubjectReference() { throw new UnsupportedOperationException(); }
    @Override public Optional<?> associatedObject() { throw new UnsupportedOperationException(); }
    @Override public Cause contextCause() { throw new UnsupportedOperationException(); }
    @Override public boolean isSubjectDataPersisted() { throw new UnsupportedOperationException(); }
    @Override public SubjectData subjectData() { throw new UnsupportedOperationException(); }
    @Override public SubjectData transientSubjectData() { throw new UnsupportedOperationException(); }
    @Override public boolean hasPermission(String permission) { throw new UnsupportedOperationException(); }
    @Override public boolean hasPermission(String permission, Cause cause) { throw new UnsupportedOperationException(); }
    @Override public boolean hasPermission(String permission, Set<Context> contexts) { throw new UnsupportedOperationException(); }
    @Override public Tristate permissionValue(String permission) { throw new UnsupportedOperationException(); }
    @Override public Tristate permissionValue(String permission, Cause cause) { throw new UnsupportedOperationException(); }
    @Override public Tristate permissionValue(String permission, Set<Context> contexts) { throw new UnsupportedOperationException(); }
    @Override public boolean isChildOf(SubjectReference parent) { throw new UnsupportedOperationException(); }
    @Override public boolean isChildOf(SubjectReference parent, Cause cause) { throw new UnsupportedOperationException(); }
    @Override public boolean isChildOf(SubjectReference parent, Set<Context> contexts) { throw new UnsupportedOperationException(); }
    @Override public List<? extends SubjectReference> parents() { throw new UnsupportedOperationException(); }
    @Override public List<? extends SubjectReference> parents(Cause cause) { throw new UnsupportedOperationException(); }
    @Override public List<? extends SubjectReference> parents(Set<Context> contexts) { throw new UnsupportedOperationException(); }
    @Override public Optional<String> option(String key) { throw new UnsupportedOperationException(); }
    @Override public Optional<String> option(String key, Cause cause) { throw new UnsupportedOperationException(); }
    @Override public Optional<String> option(String key, Set<Context> contexts) { throw new UnsupportedOperationException(); }
    @Override public String identifier() { throw new UnsupportedOperationException(); }
    @Override public Optional<String> friendlyIdentifier() { throw new UnsupportedOperationException(); }
}
