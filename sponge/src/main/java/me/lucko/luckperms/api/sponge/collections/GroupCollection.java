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

package me.lucko.luckperms.api.sponge.collections;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.lucko.luckperms.api.sponge.LuckPermsService;
import me.lucko.luckperms.api.sponge.LuckPermsSubject;
import me.lucko.luckperms.api.sponge.simple.SimpleSubject;
import me.lucko.luckperms.groups.GroupManager;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public class GroupCollection implements SubjectCollection {
    private final LuckPermsService service;
    private final GroupManager manager;

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_GROUP;
    }

    @Override
    public Subject get(@NonNull String id) {
        if (manager.isLoaded(id)) {
            return LuckPermsSubject.wrapHolder(manager.get(id), service);
        }

        return new SimpleSubject(id, service, this);
    }

    @Override
    public boolean hasRegistered(@NonNull String id) {
        return manager.isLoaded(id);
    }

    @Override
    public Iterable<Subject> getAllSubjects() {
        return manager.getAll().values().stream()
                .map(u -> LuckPermsSubject.wrapHolder(u, service))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull String node) {
        return getAllWithPermission(SubjectData.GLOBAL_CONTEXT, node);
    }

    @Override
    public Map<Subject, Boolean> getAllWithPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        return manager.getAll().values().stream()
                .map(u -> LuckPermsSubject.wrapHolder(u, service))
                .filter(sub -> sub.hasPermission(contexts, node))
                .collect(Collectors.toMap(sub -> sub, sub -> sub.getPermissionValue(contexts, node).asBoolean()));
    }

    @Override
    public Subject getDefaults() {
        return new SimpleSubject("default", service, this);
    }
}
