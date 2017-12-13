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

package me.lucko.luckperms.api;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Wrapper around parts of the LuckPerms configuration file
 */
public interface LPConfiguration {

    /**
     * Gets the name of this server
     *
     * @return the name of this server
     */
    @Nonnull
    String getServer();

    /**
     * Gets if the users on this server will have their global permissions applied
     *
     * @return if the users on this server will have their global permissions applied
     */
    boolean getIncludeGlobalPerms();

    /**
     * Gets if the users on this server will have their global world permissions applied
     *
     * @return if the users on this server will have their global world permissions applied
     * @since 2.9
     */
    boolean getIncludeGlobalWorldPerms();

    /**
     * Gets if the platform is applying global groups
     *
     * @return true if the platform is applying global groups
     * @since 2.9
     */
    boolean getApplyGlobalGroups();

    /**
     * Gets if the platform is applying global world groups
     *
     * @return true if the platform is applying global world groups
     * @since 2.9
     */
    boolean getApplyGlobalWorldGroups();

    /**
     * Gets the storage method string from the configuration
     *
     * @return the storage method string from the configuration
     */
    @Nonnull
    String getStorageMethod();

    /**
     * Gets true if split storage is enabled
     *
     * @return true if split storage is enabled
     * @since 2.7
     */
    boolean getSplitStorage();

    /**
     * Gets a map of split storage options
     *
     * @return a map of split storage options, where the key is the storage section, and the value is the storage
     * method. For example: key = user, value = json
     * @since 2.7
     */
    @Nonnull
    Map<String, String> getSplitStorageOptions();

    @Nonnull
    Unsafe unsafe();

    interface Unsafe {

        /**
         * Gets an Object from the config.
         *
         * <p>This method is nested under {@link Unsafe} because the keys
         * and return types may change between versions without warning.</p>
         *
         * @param key the key, as defined as a field name in
         *            the "ConfigKeys" class.
         * @return the corresponding object, if one is present
         * @throws IllegalArgumentException if the key isn't known
         */
        @Nonnull
        Object getObject(String key);
    }

}
