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

package net.luckperms.api.event.player.lookup;

import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.util.Param;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Called when the validity of a username is being tested.
 *
 * @since 5.3
 */
public interface UsernameValidityCheckEvent extends LuckPermsEvent {

    /**
     * Gets the username being tested.
     *
     * @return the username
     */
    @Param(0)
    @NonNull String getUsername();

    /**
     * Gets the current validity state for the username.
     *
     * @return the validity state
     */
    @Param(1)
    @NonNull AtomicBoolean validityState();

    /**
     * Gets if the username is currently considered to be valid.
     *
     * @return if the username is valid
     */
    default boolean isValid() {
        return validityState().get();
    }

    /**
     * Sets if the username should be considered valid or not.
     *
     * @param valid whether the username is valid
     */
    default void setValid(boolean valid) {
        validityState().set(valid);
    }

}
