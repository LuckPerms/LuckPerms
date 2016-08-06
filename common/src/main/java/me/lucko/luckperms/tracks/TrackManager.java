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

import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TrackManager {

    /**
     * A {@link Map} containing all loaded tracks
     */
    @Getter
    private final Map<String, Track> tracks = new ConcurrentHashMap<>();

    /**
     * Get a track object by name
     * @param name the name to search by
     * @return a {@link Track} object if the track is loaded, else returns null
     */
    public Track getTrack(String name) {
        return tracks.get(name);
    }

    /**
     * Returns a set of tracks that contain at least one of the groups from the Set provided
     * @param group the group to filter by
     * @return a set of tracks that the groups could be a member of
     */
    public Set<Track> getApplicableTracks(String group) {
        return tracks.values().stream().filter(t -> t.containsGroup(group)).collect(Collectors.toSet());
    }

    /**
     * Add a track to the loaded tracks map
     * @param track The track to add
     */
    public void setTrack(Track track) {
        tracks.put(track.getName(), track);
    }

    /**
     * Updates (or sets if the track wasn't already loaded) a track in the tracks map
     * @param track The track to update or set
     */
    public void updateOrSetTrack(Track track) {
        if (!isLoaded(track.getName())) {
            // The track isn't already loaded
            tracks.put(track.getName(), track);
        } else {
            tracks.get(track.getName()).setGroups(track.getGroups());
        }
    }

    /**
     * Check to see if a track is loaded or not
     * @param name The name of the track
     * @return true if the track is loaded
     */
    public boolean isLoaded(String name) {
        return tracks.containsKey(name);
    }

    /**
     * Removes and unloads the track from the plugins internal storage
     * @param track The track to unload
     */
    public void unloadTrack(Track track) {
        if (track != null) {
            tracks.remove(track.getName());
        }
    }

    /**
     * Unloads all tracks from the manager
     */
    public void unloadAll() {
        tracks.clear();
    }

    /**
     * Makes a new track object
     * @param name The name of the track
     * @return a new {@link Track} object
     */
    public Track makeTrack(String name) {
        return new Track(name);
    }
}
