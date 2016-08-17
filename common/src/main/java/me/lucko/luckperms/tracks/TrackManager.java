/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.tracks;

import me.lucko.luckperms.utils.AbstractManager;

import java.util.Set;
import java.util.stream.Collectors;

public class TrackManager extends AbstractManager<String, Track> {

    /**
     * Returns a set of tracks that contain at least one of the groups from the Set provided
     * @param group the group to filter by
     * @return a set of tracks that the groups could be a member of
     */
    public Set<Track> getApplicableTracks(String group) {
        return objects.values().stream()
                .filter(t -> t.containsGroup(group))
                .collect(Collectors.toSet());
    }

    @Override
    protected void copy(Track from, Track to) {
        to.setGroups(from.getGroups());
    }

    /**
     * Makes a new track object
     * @param name The name of the track
     * @return a new {@link Track} object
     */
    @Override
    public Track make(String name) {
        return new Track(name);
    }
}
