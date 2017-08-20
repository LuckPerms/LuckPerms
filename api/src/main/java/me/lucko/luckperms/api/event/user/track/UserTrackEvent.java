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

package me.lucko.luckperms.api.event.user.track;

import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.event.LuckPermsEvent;

import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Called when a user interacts with a track through a promotion or demotion
 */
public interface UserTrackEvent extends LuckPermsEvent {

    /**
     * Gets the track involved in the event
     *
     * @return the track involved in the event
     */
    @Nonnull
    Track getTrack();

    /**
     * Gets the user who was promoted or demoted
     *
     * @return the user involved in the event
     */
    @Nonnull
    User getUser();

    /**
     * Gets the action performed
     *
     * @return the action performed
     */
    @Nonnull
    TrackAction getAction();

    /**
     * Gets the group the user was promoted/demoted from.
     *
     * <p>May be {@link Optional#empty()} if the user wasn't already placed on the track.</p>
     *
     * @return the group the user was promoted/demoted from
     */
    @Nonnull
    Optional<String> getGroupFrom();

    /**
     * Gets the group the user was promoted/demoted to
     *
     * @return the group the user was promoted/demoted to
     */
    @Nonnull
    Optional<String> getGroupTo();

}
