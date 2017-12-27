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

package me.lucko.luckperms.common.event.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.event.log.LogBroadcastEvent;
import me.lucko.luckperms.api.event.log.LogNotifyEvent;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.event.AbstractEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@ToString
@RequiredArgsConstructor
public class EventLogNotify extends AbstractEvent implements LogNotifyEvent {

    private final AtomicBoolean cancellationState;
    private final LogEntry entry;
    private final LogBroadcastEvent.Origin origin;
    private final Sender sender;

    @Getter(AccessLevel.NONE)
    private Notifiable notifiable;

    @Override
    public synchronized Notifiable getNotifiable() {
        if (notifiable == null) {
            notifiable = new SenderNotifiable(sender);
        }
        return notifiable;
    }

    @AllArgsConstructor
    private static final class SenderNotifiable implements Notifiable {
        private final Sender sender;

        @Override
        public Optional<UUID> getUuid() {
            if (sender.isConsole()) {
                return Optional.empty();
            }
            return Optional.of(sender.getUuid());
        }

        @Override
        public String getName() {
            return sender.getName();
        }

        @Override
        public boolean isConsole() {
            return sender.isConsole();
        }

        @Override
        public boolean isPlayer() {
            return !sender.isConsole();
        }
    }

}
