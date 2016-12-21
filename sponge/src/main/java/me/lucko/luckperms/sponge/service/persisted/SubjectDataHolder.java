/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.sponge.service.persisted;

import lombok.ToString;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.references.SubjectReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Holds SubjectData in a "gson friendly" format for serialization
 */
@ToString
public class SubjectDataHolder {
    private final Map<Map<String, String>, Map<String, Boolean>> permissions;
    private final Map<Map<String, String>, Map<String, String>> options;
    private final Map<Map<String, String>, List<String>> parents;

    public SubjectDataHolder(Map<ImmutableContextSet, Map<String, String>> options, Map<ImmutableContextSet, Map<String, Boolean>> permissions, Map<ImmutableContextSet, Set<SubjectReference>> parents) {
        this.options = new HashMap<>();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : options.entrySet()) {
            if (!e.getValue().isEmpty()) {
                this.options.put(e.getKey().toMap(), new HashMap<>(e.getValue()));
            }
        }

        this.permissions = new HashMap<>();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : permissions.entrySet()) {
            if (!e.getValue().isEmpty()) {
                this.permissions.put(e.getKey().toMap(), new HashMap<>(e.getValue()));
            }
        }

        this.parents = new HashMap<>();
        for (Map.Entry<ImmutableContextSet, Set<SubjectReference>> e : parents.entrySet()) {
            if (!e.getValue().isEmpty()) {
                this.parents.put(e.getKey().toMap(), e.getValue().stream().map(SubjectReference::serialize).collect(Collectors.toList()));
            }
        }
    }

    public SubjectDataHolder(CalculatedSubjectData data) {
        this(data.getOptions(), data.getPermissions(), data.getParents());
    }

    public void copyTo(CalculatedSubjectData subjectData) {
        subjectData.replacePermissions(permissions.entrySet().stream()
                .collect(Collectors.toMap(
                        k -> ContextSet.fromMap(k.getKey()),
                        Map.Entry::getValue
                ))
        );

        subjectData.replaceOptions(options.entrySet().stream()
                .collect(Collectors.toMap(
                        k -> ContextSet.fromMap(k.getKey()),
                        Map.Entry::getValue
                ))
        );

        subjectData.replaceParents(parents.entrySet().stream()
                .collect(Collectors.toMap(
                        k -> ContextSet.fromMap(k.getKey()),
                        v -> v.getValue().stream().map(SubjectReference::deserialize).collect(Collectors.toSet())
                ))
        );
    }
}
