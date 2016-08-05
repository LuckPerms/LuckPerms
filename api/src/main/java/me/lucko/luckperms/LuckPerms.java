package me.lucko.luckperms;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.lucko.luckperms.api.LuckPermsApi;

import java.util.Optional;

/**
 * Static access to LuckPerms
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LuckPerms {

    private static LuckPermsApi api = null;


    /**
     * Gets an instance of {@link LuckPermsApi}
     * @return an api instance
     * @throws IllegalStateException if the api is not loaded
     */
    public static LuckPermsApi getApi() {
        if (api == null) {
            throw new IllegalStateException("API is not loaded.");
        }
        return api;
    }

    /**
     * Gets an instance of {@link LuckPermsApi} safely. Unlike {@link LuckPerms#getApi}, this method will not throw an
     * {@link IllegalStateException} if the api is not loaded, rather return an empty {@link Optional}.
     * @return an optional api instance
     */
    public static Optional<LuckPermsApi> getApiSafe() {
        return Optional.ofNullable(api);
    }

    static void registerProvider(LuckPermsApi luckPermsApi) {
        api = luckPermsApi;
    }

    static void unregisterProvider() {
        api = null;
    }

}
