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

package net.luckperms.api.actionlog.filter;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.actionlog.Action;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * A predicate filter which matches certain {@link Action}s.
 *
 * <p>API users should not implement this interface directly.</p>
 *
 * @since 5.5
 */
@NonExtendable
public interface ActionFilter extends Predicate<Action> {

    /**
     * Gets an {@link ActionFilter} which matches any action.
     *
     * @return the matcher
     */
    static ActionFilter any() {
        return LuckPermsProvider.get().getActionFilterFactory().any();
    }

    /**
     * Gets an {@link ActionFilter} which matches actions with a specific source user.
     *
     * @param uniqueId the source user unique id
     * @return the matcher
     */
    static ActionFilter source(UUID uniqueId) {
        return LuckPermsProvider.get().getActionFilterFactory().source(uniqueId);
    }

    /**
     * Gets an {@link ActionFilter} which matches actions which target a specific user.
     *
     * @param uniqueId the target user unique id
     * @return the matcher
     */
    static ActionFilter user(UUID uniqueId) {
        return LuckPermsProvider.get().getActionFilterFactory().user(uniqueId);
    }

    /**
     * Gets an {@link ActionFilter} which matches actions which target a specific group.
     *
     * @param name the target group name
     * @return the matcher
     */
    static ActionFilter group(String name) {
        return LuckPermsProvider.get().getActionFilterFactory().group(name);
    }

    /**
     * Gets an {@link ActionFilter} which matches actions which target a specific track.
     *
     * @param name the target track name
     * @return the matcher
     */
    static ActionFilter track(String name) {
        return LuckPermsProvider.get().getActionFilterFactory().track(name);
    }

    /**
     * Gets an {@link ActionFilter} which matches actions which contain a specific search query in the source name,
     * target name or description.
     *
     * @param query the search query
     * @return the matcher
     */
    static ActionFilter search(String query) {
        return LuckPermsProvider.get().getActionFilterFactory().search(query);
    }

    /**
     * Tests to see if the given {@link Action} matches the filter.
     *
     * @param action the action to test
     * @return true if the action matched
     */
    @Override
    boolean test(Action action);

}
