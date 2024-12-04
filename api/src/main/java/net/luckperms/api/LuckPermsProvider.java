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

package net.luckperms.api;

import org.checkerframework.checker.nullness.qual.NonNull;

import static org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Provides static access to the {@link LuckPerms} API.
 *
 * <p>Ideally, the ServiceManager for the platform should be used to obtain an
 * instance, however, this provider can be used if this is not viable.</p>
 */
public final class LuckPermsProvider {
    private static LuckPerms instance = null;

    /**
     * Gets an instance of the {@link LuckPerms} API,
     * throwing {@link IllegalStateException} if the API is not loaded yet.
     *
     * <p>This method will never return null.</p>
     *
     * @return an instance of the LuckPerms API
     * @throws IllegalStateException if the API is not loaded yet
     */
    public static @NonNull LuckPerms get() {
        LuckPerms instance = LuckPermsProvider.instance;
        if (instance == null) {
            throw new NotLoadedException();
        }
        return instance;
    }

    @Internal
    static void register(LuckPerms instance) {
        LuckPermsProvider.instance = instance;
    }

    @Internal
    static void unregister() {
        LuckPermsProvider.instance = null;
    }

    @Internal
    private LuckPermsProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Exception thrown when the API is requested before it has been loaded.
     */
    private static final class NotLoadedException extends IllegalStateException {
        private static final String MESSAGE = "The LuckPerms API isn't loaded yet!\n" +
                "This could be because:\n" +
                "  a) the LuckPerms plugin is not installed or it failed to enable\n" +
                "  b) the plugin in the stacktrace does not declare a dependency on LuckPerms\n" +
                "  c) the plugin in the stacktrace is retrieving the API before the plugin 'enable' phase\n" +
                "     (call the #get method in onEnable, not the constructor!)\n" +
                "  d) the plugin in the stacktrace is incorrectly 'shading' the LuckPerms API into its jar\n";

        NotLoadedException() {
            super(MESSAGE);
        }
    }

}
