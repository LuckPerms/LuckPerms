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

package me.lucko.luckperms.sponge.service.model;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.Subject;

import java.util.Optional;

/**
 * LuckPerms model for the Sponge {@link Subject}
 */
public interface LPSubject {

    Subject sponge();

    LPPermissionService getService();

    String getIdentifier();

    default SubjectReference toReference() {
        return SubjectReferenceFactory.obtain(getService(), this);
    }

    default LPSubject getDefaults() {
        return getService().getDefaultSubjects().loadSubject(getIdentifier()).join();
    }

    default Optional<String> getFriendlyIdentifier() {
        return Optional.empty();
    }

    default Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }

    LPSubjectCollection getParentCollection();

    LPSubjectData getSubjectData();

    LPSubjectData getTransientSubjectData();

    Tristate getPermissionValue(ImmutableContextSet contexts, String permission);

    boolean isChildOf(ImmutableContextSet contexts, SubjectReference parent);

    ImmutableList<SubjectReference> getParents(ImmutableContextSet contexts);

    Optional<String> getOption(ImmutableContextSet contexts, String key);

    ImmutableContextSet getActiveContextSet();

    default void performCleanup() {

    }

    void invalidateCaches(CacheLevel cacheLevel);

    /**
     * The level of cache for invalidation
     *
     * Invalidating at {@link #PARENT} will also invalidate at
     * {@link #PERMISSION} and {@link #OPTION}, and invalidating at
     * {@link #PERMISSION} will also invalidate at {@link #OPTION}.
     */
    enum CacheLevel {
        PARENT, PERMISSION, OPTION
    }

}
