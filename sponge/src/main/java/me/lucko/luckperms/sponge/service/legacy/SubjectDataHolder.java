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

package me.lucko.luckperms.sponge.service.legacy;

import lombok.ToString;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.SubjectReferenceFactory;
import me.lucko.luckperms.sponge.service.storage.SubjectStorageModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @deprecated Because this format is no longer being used to store data.
 * @see SubjectStorageModel
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@ToString
@Deprecated
public class SubjectDataHolder {
    private Map<Map<String, String>, Map<String, Boolean>> permissions;
    private Map<Map<String, String>, Map<String, String>> options;
    private Map<Map<String, String>, List<String>> parents;

    public SubjectDataHolder() {
        // For gson
    }

    public SubjectStorageModel asSubjectModel(LPPermissionService service) {
        return new SubjectStorageModel(service,
                permissions.entrySet().stream()
                        .collect(Collectors.toMap(
                                k -> ImmutableContextSet.fromMap(k.getKey()),
                                Map.Entry::getValue
                        )),
                options.entrySet().stream()
                        .collect(Collectors.toMap(
                                k -> ImmutableContextSet.fromMap(k.getKey()),
                                Map.Entry::getValue
                        )),
                parents.entrySet().stream()
                        .collect(Collectors.toMap(
                                k -> ImmutableContextSet.fromMap(k.getKey()),
                                v -> v.getValue().stream().map(s -> SubjectReferenceFactory.deserialize(service, s)).collect(Collectors.toList())
                        ))
        );
    }
}
