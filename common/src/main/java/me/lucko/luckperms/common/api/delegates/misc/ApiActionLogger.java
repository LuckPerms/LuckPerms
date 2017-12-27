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

package me.lucko.luckperms.common.api.delegates.misc;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.ActionLogger;
import me.lucko.luckperms.api.Log;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.api.delegates.model.ApiLog;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class ApiActionLogger implements ActionLogger {
    private final LuckPermsPlugin plugin;

    @Override
    public LogEntry.Builder newEntryBuilder() {
        return ExtendedLogEntry.build();
    }

    @Override
    public CompletableFuture<Log> getLog() {
        return plugin.getStorage().noBuffer().getLog().thenApply(log -> log == null ? null : new ApiLog(log));
    }

    @Override
    public CompletableFuture<Void> submit(LogEntry entry) {
        return CompletableFuture.runAsync(() -> plugin.getLogDispatcher().dispatchFromApi((ExtendedLogEntry) entry), plugin.getScheduler().async());
    }

    @Override
    public CompletableFuture<Void> submitToStorage(LogEntry entry) {
        return plugin.getStorage().noBuffer().logAction(entry);
    }

    @Override
    public CompletableFuture<Void> broadcastAction(LogEntry entry) {
        return CompletableFuture.runAsync(() -> plugin.getLogDispatcher().broadcastFromApi((ExtendedLogEntry) entry), plugin.getScheduler().async());
    }
}
