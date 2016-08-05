package me.lucko.luckperms.api.implementation.internal;

import lombok.experimental.UtilityClass;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;

@UtilityClass
class Utils {

    static void checkUser(User user) {
        if (!(user instanceof UserLink)) {
            throw new IllegalArgumentException("User instance cannot be handled by this implementation.");
        }
    }

    static void checkGroup(Group group) {
        if (!(group instanceof GroupLink)) {
            throw new IllegalArgumentException("Group instance cannot be handled by this implementation.");
        }
    }

    static void checkTrack(Track track) {
        if (!(track instanceof TrackLink)) {
            throw new IllegalArgumentException("Track instance cannot be handled by this implementation.");
        }
    }

}
