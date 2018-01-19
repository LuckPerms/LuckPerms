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

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.event.log.LogBroadcastEvent;
import me.lucko.luckperms.api.event.log.LogNotifyEvent;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.event.AbstractEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

public class EventLogNotify extends AbstractEvent implements LogNotifyEvent {

    private final AtomicBoolean cancellationState;
    private final LogEntry entry;
    private final LogBroadcastEvent.Origin origin;
    private final Sender sender;

    private Notifiable notifiable;

    public EventLogNotify(AtomicBoolean cancellationState, LogEntry entry, LogBroadcastEvent.Origin origin, Sender sender) {
        this.cancellationState = cancellationState;
        this.entry = entry;
        this.origin = origin;
        this.sender = sender;
    }

    @Nonnull
    @Override
    public synchronized Notifiable getNotifiable() {
        if (this.notifiable == null) {
            this.notifiable = new SenderNotifiable(this.sender);
        }
        return this.notifiable;
    }

    @Nonnull
    @Override
    public AtomicBoolean getCancellationState() {
        return this.cancellationState;
    }

    @Nonnull
    @Override
    public LogEntry getEntry() {
        return this.entry;
    }

    @Nonnull
    @Override
    public LogBroadcastEvent.Origin getOrigin() {
        return this.origin;
    }

    public Sender getSender() {
        return this.sender;
    }

    @Override
    public String toString() {
        return "EventLogNotify(cancellationState=" + this.getCancellationState() + ", entry=" + this.getEntry() + ", origin=" + this.getOrigin() + ", sender=" + this.getSender() + ", notifiable=" + this.getNotifiable() + ")";
    }

    private static final class SenderNotifiable implements Notifiable {
        private final Sender sender;

        public SenderNotifiable(Sender sender) {
            this.sender = sender;
        }

        @Nonnull
        @Override
        public Optional<UUID> getUuid() {
            if (this.sender.isConsole()) {
                return Optional.empty();
            }
            return Optional.of(this.sender.getUuid());
        }

        @Nonnull
        @Override
        public String getName() {
            return this.sender.getName();
        }

        @Override
        public boolean isConsole() {
            return this.sender.isConsole();
        }

        @Override
        public boolean isPlayer() {
            return !this.sender.isConsole();
        }
    }

}
