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

package net.luckperms.api.event.context;

import net.luckperms.api.context.ContextManager;
import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.util.Param;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * Called when a subject's current/active contexts are updated.
 *
 * <p>There are no guarantees that this event will be called for every update. It is merely to be
 * used as a "hint" for plugins which depend on the current active contexts for a player.</p>
 *
 * <p>It will always be fired following a call to
 * {@link ContextManager#signalContextUpdate(Object)}.</p>
 *
 * <p>The {@link #getSubject() subject} is always an instance of the platform's subject type. See
 * {@link ContextManager} for details.</p>
 *
 * <p>Unlike most other LuckPerms events, this event is not fired asynchronously. Care should be
 * taken to ensure listeners are lightweight. Additionally, listeners should ensure they do not
 * cause further updates to player context, thus possibly causing a stack overflow.</p>
 *
 * @since 5.2
 */
public interface ContextUpdateEvent extends LuckPermsEvent {

    /**
     * Gets the subject whose contexts were updated.
     *
     * @return the subject
     */
    @Param(0)
    @NonNull Object getSubject();

    /**
     * Gets the subject whose contexts were updated, casted to a given type.
     *
     * @param subjectClass the type to cast to
     * @param <T> the subject type
     * @return the casted subject
     */
    default <T> @NonNull Optional<T> getSubject(@NonNull Class<T> subjectClass) {
        Object subject = getSubject();
        return subjectClass.isInstance(subject) ? Optional.of(subjectClass.cast(subject)) : Optional.empty();
    }

}
