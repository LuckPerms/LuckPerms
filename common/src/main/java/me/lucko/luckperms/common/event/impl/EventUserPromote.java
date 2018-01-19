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

import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.event.user.track.TrackAction;
import me.lucko.luckperms.api.event.user.track.UserPromoteEvent;
import me.lucko.luckperms.common.event.AbstractEvent;

import java.util.Optional;

import javax.annotation.Nonnull;

public class EventUserPromote extends AbstractEvent implements UserPromoteEvent {

    private final Track track;
    private final User user;

    private final String groupFrom;
    private final String groupTo;

    public EventUserPromote(Track track, User user, String groupFrom, String groupTo) {
        this.track = track;
        this.user = user;
        this.groupFrom = groupFrom;
        this.groupTo = groupTo;
    }

    @Nonnull
    @Override
    public Track getTrack() {
        return this.track;
    }

    @Nonnull
    @Override
    public User getUser() {
        return this.user;
    }

    @Nonnull
    @Override
    public TrackAction getAction() {
        return TrackAction.PROMOTION;
    }

    @Nonnull
    @Override
    public Optional<String> getGroupFrom() {
        return Optional.ofNullable(this.groupFrom);
    }

    @Nonnull
    @Override
    public Optional<String> getGroupTo() {
        return Optional.ofNullable(this.groupTo);
    }

    @Override
    public String toString() {
        return "EventUserPromote(track=" + this.track + ", user=" + this.user + ", groupFrom=" + this.getGroupFrom() + ", groupTo=" + this.getGroupTo() + ")";
    }

}
