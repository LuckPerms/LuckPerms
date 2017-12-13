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

package me.lucko.luckperms.common.api.delegates.manager;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.manager.TrackManager;

import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ApiTrackManager implements TrackManager {
    private final me.lucko.luckperms.common.managers.TrackManager handle;

    @Override
    public Track getTrack(@NonNull String name) {
        me.lucko.luckperms.common.model.Track track = handle.getIfLoaded(name);
        return track == null ? null : track.getDelegate();
    }

    @Override
    public Set<Track> getLoadedTracks() {
        return handle.getAll().values().stream().map(me.lucko.luckperms.common.model.Track::getDelegate).collect(Collectors.toSet());
    }

    @Override
    public boolean isLoaded(@NonNull String name) {
        return handle.isLoaded(name);
    }
}
