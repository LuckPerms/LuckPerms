package me.lucko.luckperms.api.implementation.internal;

import lombok.experimental.UtilityClass;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.utils.DateUtil;
import me.lucko.luckperms.utils.Patterns;

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

    static String checkUsername(String s) {
        if (s.length() > 16 || Patterns.NON_USERNAME.matcher(s).find()) {
            throw new IllegalArgumentException("Invalid username entry '" + s + "'. Usernames must be less than 16 chars" +
                    " and only contain 'a-z A-Z 1-9 _'.");
        }
        return s;
    }

    static String checkName(String s) {
        if (s.length() > 36 || Patterns.NON_ALPHA_NUMERIC.matcher(s).find()) {
            throw new IllegalArgumentException("Invalid name entry '" + s + "'. Names must be less than 37 chars" +
                    " and only contain 'a-z A-Z 1-9'.");
        }
        return s.toLowerCase();
    }

    static String checkServer(String s) {
        if (Patterns.NON_ALPHA_NUMERIC.matcher(s).find()) {
            throw new IllegalArgumentException("Invalid server entry '" + s + "'. Server names can only contain alphanumeric characters.");
        }
        return s;
    }

    static String checkNode(String s) {
        if (s.contains("/") || s.contains("$")) {
            throw new IllegalArgumentException("Invalid node entry '" + s + "'. Nodes cannot contain '/' or '$' characters.");
        }
        return s;
    }

    static long checkTime(long l) {
        if (DateUtil.shouldExpire(l)) {
            throw new IllegalArgumentException("Unix time '" + l + "' is invalid, as it has already passed.");
        }
        return l;
    }

}
