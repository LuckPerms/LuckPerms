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

import com.google.common.collect.ImmutableSortedSet;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.actionlog.ActionLog;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.UUID;

@Deprecated
public class ApiActionLog implements ActionLog {
    private final SortedSet<Action> content;

    public ApiActionLog(List<LoggedAction> content) {
        this.content = ImmutableSortedSet.copyOf(content);
    }

    @Override
    public @NonNull SortedSet<Action> getContent() {
        return this.content;
    }

    @Override
    public @NonNull SortedSet<Action> getContent(@NonNull UUID actor) {
        Objects.requireNonNull(actor, "actor");
        return this.content.stream()
                .filter(e -> e.getSource().getUniqueId().equals(actor))
                .collect(ImmutableCollectors.toSortedSet());
    }

    @Override
    public @NonNull SortedSet<Action> getUserHistory(@NonNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uuid");
        return this.content.stream()
                .filter(e -> e.getTarget().getType() == Action.Target.Type.USER)
                .filter(e -> e.getTarget().getUniqueId().isPresent() && e.getTarget().getUniqueId().get().equals(uniqueId))
                .collect(ImmutableCollectors.toSortedSet());
    }

    @Override
    public @NonNull SortedSet<Action> getGroupHistory(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.content.stream()
                .filter(e -> e.getTarget().getType() == Action.Target.Type.GROUP)
                .filter(e -> e.getTarget().getName().equals(name))
                .collect(ImmutableCollectors.toSortedSet());
    }

    @Override
    public @NonNull SortedSet<Action> getTrackHistory(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.content.stream()
                .filter(e -> e.getTarget().getType() == Action.Target.Type.TRACK)
                .filter(e -> e.getTarget().getName().equals(name))
                .collect(ImmutableCollectors.toSortedSet());
    }
}
