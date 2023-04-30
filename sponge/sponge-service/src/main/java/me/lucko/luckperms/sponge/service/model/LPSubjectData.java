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
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.util.Tristate;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.concurrent.CompletableFuture;

/**
 * LuckPerms model for the Sponge {@link SubjectData}
 */
public interface LPSubjectData {

    SubjectData sponge();

    LPSubject getParentSubject();

    DataType getType();

    /* permissions */

    ImmutableMap<ImmutableContextSet, ImmutableMap<String, Boolean>> getAllPermissions();

    ImmutableMap<String, Boolean> getPermissions(ImmutableContextSet contexts);

    CompletableFuture<Boolean> setPermission(ImmutableContextSet contexts, String permission, Tristate value);

    CompletableFuture<Boolean> clearPermissions();

    CompletableFuture<Boolean> clearPermissions(ImmutableContextSet contexts);

    /* parents */

    ImmutableMap<ImmutableContextSet, ImmutableList<LPSubjectReference>> getAllParents();

    ImmutableList<LPSubjectReference> getParents(ImmutableContextSet contexts);

    CompletableFuture<Boolean> addParent(ImmutableContextSet contexts, LPSubjectReference parent);

    CompletableFuture<Boolean> removeParent(ImmutableContextSet contexts, LPSubjectReference parent);

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
