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
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;

import java.util.concurrent.CompletableFuture;

/**
 * LuckPerms model for the Sponge {@link org.spongepowered.api.service.permission.SubjectData}
 */
public interface LPSubjectData {

    LPSubject getParentSubject();

    /* permissions */

    ImmutableMap<ImmutableContextSet, ImmutableMap<String, Boolean>> getAllPermissions();

    default ImmutableMap<String, Boolean> getPermissions(ImmutableContextSet contexts) {
        return ImmutableMap.copyOf(getAllPermissions().getOrDefault(contexts, ImmutableMap.of()));
    }

    CompletableFuture<Boolean> setPermission(ImmutableContextSet contexts, String permission, Tristate value);

    CompletableFuture<Boolean> clearPermissions();

    CompletableFuture<Boolean> clearPermissions(ImmutableContextSet contexts);

    /* parents */

    ImmutableMap<ImmutableContextSet, ImmutableList<SubjectReference>> getAllParents();

    default ImmutableList<SubjectReference> getParents(ImmutableContextSet contexts) {
        return ImmutableList.copyOf(getAllParents().getOrDefault(contexts, ImmutableList.of()));
    }

    CompletableFuture<Boolean> addParent(ImmutableContextSet contexts, SubjectReference parent);

    CompletableFuture<Boolean> removeParent(ImmutableContextSet contexts, SubjectReference parent);

    CompletableFuture<Boolean> clearParents();

    CompletableFuture<Boolean> clearParents(ImmutableContextSet contexts);

    /* options */

    ImmutableMap<ImmutableContextSet, ImmutableMap<String, String>> getAllOptions();

    default ImmutableMap<String, String> getOptions(ImmutableContextSet contexts) {
        return ImmutableMap.copyOf(getAllOptions().getOrDefault(contexts, ImmutableMap.of()));
    }

    CompletableFuture<Boolean> setOption(ImmutableContextSet contexts, String key, String value);

    CompletableFuture<Boolean> unsetOption(ImmutableContextSet contexts, String key);

    CompletableFuture<Boolean> clearOptions();

    CompletableFuture<Boolean> clearOptions(ImmutableContextSet contexts);

}
