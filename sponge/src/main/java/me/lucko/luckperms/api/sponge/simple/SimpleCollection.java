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

package me.lucko.luckperms.api.sponge.simple;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class SimpleCollection implements SubjectCollection {
    private final PermissionService service;

    @Getter
    private final String identifier;

    private final Map<String, Subject> subjects = new ConcurrentHashMap<>();

    @Override
    public Subject get(@NonNull String id) {
        if (!subjects.containsKey(id)) {
            subjects.put(id, new SimpleSubject(id, service, this));
        }

        return subjects.get(id);
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        return subjects.containsKey(id);
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return subjects.values();
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull String id) {
        return getAllWithPermission(Collections.emptySet(), id);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        Map<Subject, Boolean> m = new HashMap<>();
        for (Subject subject : subjects.values()) {
            Tristate ts = subject.getPermissionValue(contexts, node);
            if (ts != Tristate.UNDEFINED) {
                m.put(subject, ts.asBoolean());
            }

        }
        return m;
    }

    @Override
    public Subject getDefaults() {
        return new SimpleSubject("default", service, this);
    }
}
