package me.lucko.luckperms;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.lucko.luckperms.api.LuckPermsApi;

/**
 * Static access to LuckPerms
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LuckPerms {

    private static LuckPermsApi api = null;

    public static LuckPermsApi getApi() {
        if (api == null) {
            throw new IllegalStateException("API is not loaded.");
        }
        return api;
    }

    static void registerProvider(LuckPermsApi luckPermsApi) {
        api = luckPermsApi;
    }

    static void unregisterProvider() {
        api = null;
    }

}
