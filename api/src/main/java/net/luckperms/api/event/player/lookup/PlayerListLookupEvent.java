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

import java.util.Set;

/**
 * Called when the platform needs a list of player usernames, for example when
 * tab completing a command argument.
 *
 * <p>The set initially contains the players connected to this server/proxy.
 * Listeners can add or remove names, e.g. to include players connected to
 * other servers on the network.</p>
 *
 * @since 5.6
 */
public interface PlayerListLookupEvent extends LuckPermsEvent {

    /**
     * Gets the mutable set of player usernames.
     *
     * @return the usernames
     */
    @Param(0)
    Set<String> getUsernames();

}
