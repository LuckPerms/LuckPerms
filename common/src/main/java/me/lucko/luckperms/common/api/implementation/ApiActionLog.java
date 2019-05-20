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

import me.lucko.luckperms.api.actionlog.Action;
import me.lucko.luckperms.api.actionlog.ActionLog;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.api.ApiUtils;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.SortedSet;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class ApiActionLog implements ActionLog {
    private final Log handle;

    public ApiActionLog(Log handle) {
        this.handle = handle;
    }

    @Override
    public @NonNull SortedSet<Action> getContent() {
        return (SortedSet) this.handle.getContent();
    }

    @Override
    public @NonNull SortedSet<Action> getContent(@NonNull UUID actor) {
        Objects.requireNonNull(actor, "actor");
        return (SortedSet) this.handle.getContent(actor);
    }

    @Override
    public @NonNull SortedSet<Action> getUserHistory(@NonNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return (SortedSet) this.handle.getUserHistory(uuid);
    }

    @Override
    public @NonNull SortedSet<Action> getGroupHistory(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return (SortedSet) this.handle.getGroupHistory(ApiUtils.checkName(name));
    }

    @Override
    public @NonNull SortedSet<Action> getTrackHistory(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return (SortedSet) this.handle.getTrackHistory(ApiUtils.checkName(name));
    }
}
