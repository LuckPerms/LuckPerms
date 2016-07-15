package me.lucko.luckperms.tracks;

import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TrackManager {
    private final LuckPermsPlugin plugin;

    /**
     * A {@link Map} containing all loaded tracks
     */
    @Getter
    private final Map<String, Track> tracks = new ConcurrentHashMap<>();

    public TrackManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

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
     * Load all tracks from the datastore
     */
    public void loadAllTracks() {
        plugin.getDatastore().loadAllTracks(success -> {});
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
