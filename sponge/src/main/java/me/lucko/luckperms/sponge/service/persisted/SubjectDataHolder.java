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

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.MemorySubjectData;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.stream.Collectors;

import static me.lucko.luckperms.sponge.service.LuckPermsService.convertContexts;

/**
 * Holds SubjectData in a "gson friendly" format for serialization
 */
public class SubjectDataHolder {
    private final Map<Map<String, String>, Map<String, Boolean>> permissions;
    private final Map<Map<String, String>, Map<String, String>> options;
    private final Map<Map<String, String>, List<Map.Entry<String, String>>> parents;

    public SubjectDataHolder(Map<Set<Context>, Map<String, String>> options, Map<Set<Context>, Map<String, Boolean>> permissions, Map<Set<Context>, List<Map.Entry<String, String>>> parents) {
        this.options = new HashMap<>();
        for (Map.Entry<Set<Context>, Map<String, String>> e : options.entrySet()) {
            this.options.put(convertContexts(e.getKey()), new HashMap<>(e.getValue()));
        }

        this.permissions = new HashMap<>();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> e : permissions.entrySet()) {
            this.permissions.put(convertContexts(e.getKey()), new HashMap<>(e.getValue()));
        }

        this.parents = new HashMap<>();
        for (Map.Entry<Set<Context>, List<Map.Entry<String, String>>> e : parents.entrySet()) {
            this.parents.put(convertContexts(e.getKey()), e.getValue().stream().map(p -> new AbstractMap.SimpleEntry<>(p.getKey(), p.getValue())).collect(Collectors.toList()));
        }
    }

    public SubjectDataHolder(MemorySubjectData data) {
        this(
                data.getAllOptions(),
                data.getAllPermissions(),
                data.getAllParents().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(s -> new AbstractMap.SimpleEntry<>(
                                                s.getContainingCollection().getIdentifier(),
                                                s.getIdentifier())
                                        )
                                        .collect(Collectors.toList())
                                )
                        )
        );
    }

    public void copyTo(MemorySubjectData subjectData, PermissionService service) {
        for (Map.Entry<Map<String, String>, Map<String, Boolean>> e : permissions.entrySet()) {
            for (Map.Entry<String, Boolean> perm : e.getValue().entrySet()) {
                subjectData.setPermission(convertContexts(e.getKey()), perm.getKey(), Tristate.fromBoolean(perm.getValue()));
            }
        }

        for (Map.Entry<Map<String, String>, Map<String, String>> e : options.entrySet()) {
            for (Map.Entry<String, String> option : e.getValue().entrySet()) {
                subjectData.setOption(convertContexts(e.getKey()), option.getKey(), option.getValue());
            }
        }

        for (Map.Entry<Map<String, String>, List<Map.Entry<String, String>>> e : parents.entrySet()) {
            for (Map.Entry<String, String> parent : e.getValue()) {
                subjectData.addParent(convertContexts(e.getKey()), service.getSubjects(parent.getKey()).get(parent.getValue()));
            }
        }
    }
}
