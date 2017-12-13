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

package me.lucko.luckperms.api.manager;

import me.lucko.luckperms.api.Track;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the object responsible for managing {@link Track} instances.
 *
 * @since 4.0
 */
public interface TrackManager {

    /**
     * Gets a wrapped track object from the track storage
     *
     * @param name the name of the track to get
     * @return a {@link Track} object, if one matching the name exists, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    Track getTrack(@Nonnull String name);

    /**
     * Gets a wrapped track object from the track storage.
     *
     * <p>This method does not return null, unlike {@link #getTrack}</p>
     *
     * @param name the name of the track to get
     * @return an optional {@link Track} object
     * @throws NullPointerException if the name is null
     */
    @Nonnull
    default Optional<Track> getTrackOpt(@Nonnull String name) {
        return Optional.ofNullable(getTrack(name));
    }

    /**
     * Gets a set of all loaded tracks.
     *
     * @return a {@link Set} of {@link Track} objects
     */
    @Nonnull
    Set<Track> getLoadedTracks();

    /**
     * Check if a track is loaded in memory
     *
     * @param name the name to check for
     * @return true if the track is loaded
     * @throws NullPointerException if the name is null
     */
    boolean isLoaded(@Nonnull String name);

}
