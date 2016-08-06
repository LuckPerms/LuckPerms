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
