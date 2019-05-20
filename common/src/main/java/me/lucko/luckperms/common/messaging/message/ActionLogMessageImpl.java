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

package me.lucko.luckperms.common.messaging.message;

import com.google.gson.JsonElement;

import me.lucko.luckperms.api.actionlog.Action;
import me.lucko.luckperms.api.messenger.message.type.ActionLogMessage;
import me.lucko.luckperms.common.actionlog.LogEntryJsonSerializer;
import me.lucko.luckperms.common.messaging.LuckPermsMessagingService;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

public class ActionLogMessageImpl extends AbstractMessage implements ActionLogMessage {
    public static final String TYPE = "log";

    public static ActionLogMessageImpl decode(@Nullable JsonElement content, UUID id) {
        if (content == null) {
            throw new IllegalStateException("Missing content");
        }

        return new ActionLogMessageImpl(id, LogEntryJsonSerializer.deserialize(content));
    }

    private final Action logEntry;

    public ActionLogMessageImpl(UUID id, Action logEntry) {
        super(id);
        this.logEntry = logEntry;
    }

    @Override
    public @NonNull Action getAction() {
        return this.logEntry;
    }

    @Override
    public @NonNull String asEncodedString() {
        return LuckPermsMessagingService.encodeMessageAsString(
                TYPE, getId(), LogEntryJsonSerializer.serialize(this.logEntry)
        );
    }

}
