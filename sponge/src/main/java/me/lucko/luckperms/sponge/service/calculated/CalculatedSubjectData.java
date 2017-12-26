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

package me.lucko.luckperms.sponge.service.calculated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionCalculatorMetadata;
import me.lucko.luckperms.common.contexts.ContextSetComparator;
import me.lucko.luckperms.common.processors.MapProcessor;
import me.lucko.luckperms.common.processors.PermissionProcessor;
import me.lucko.luckperms.common.references.HolderType;
import me.lucko.luckperms.common.verbose.CheckOrigin;
import me.lucko.luckperms.sponge.processors.SpongeWildcardProcessor;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * In-memory implementation of {@link LPSubjectData}.
 */
@RequiredArgsConstructor
public class CalculatedSubjectData implements LPSubjectData {

    @Getter
    private final LPSubject parentSubject;

    private final LPPermissionService service;
    private final String calculatorDisplayName;

    private final Map<ImmutableContextSet, Map<String, Boolean>> permissions = new ConcurrentHashMap<>();
    private final Map<ImmutableContextSet, Set<SubjectReference>> parents = new ConcurrentHashMap<>();
    private final Map<ImmutableContextSet, Map<String, String>> options = new ConcurrentHashMap<>();

    private final LoadingCache<ImmutableContextSet, CalculatorHolder> permissionCache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<ImmutableContextSet, CalculatorHolder>() {
                @Override
                public CalculatorHolder load(ImmutableContextSet contexts) {
                    ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();
                    processors.add(new MapProcessor());
                    processors.add(new SpongeWildcardProcessor());

                    CalculatorHolder holder = new CalculatorHolder(new PermissionCalculator(service.getPlugin(), PermissionCalculatorMetadata.of(HolderType.GROUP, calculatorDisplayName, contexts), processors.build()));
                    holder.setPermissions(flattenMap(getRelevantEntries(contexts, permissions)));

                    return holder;
                }
            });

    public void cleanup() {
        permissionCache.cleanUp();
    }

    public void invalidateLookupCache() {
        permissionCache.invalidateAll();
    }

    public Tristate getPermissionValue(ImmutableContextSet contexts, String permission) {
        return permissionCache.get(contexts).getCalculator().getPermissionValue(permission, CheckOrigin.INTERNAL);
    }

    public void replacePermissions(Map<ImmutableContextSet, Map<String, Boolean>> map) {
        permissions.clear();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : map.entrySet()) {
            permissions.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        permissionCache.invalidateAll();
        service.invalidateAllCaches(LPSubject.CacheLevel.PERMISSION);
    }

    public void replaceParents(Map<ImmutableContextSet, List<SubjectReference>> map) {
        parents.clear();
        for (Map.Entry<ImmutableContextSet, List<SubjectReference>> e : map.entrySet()) {
            Set<SubjectReference> set = ConcurrentHashMap.newKeySet();
            set.addAll(e.getValue());
            parents.put(e.getKey(), set);
        }
        service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
    }

    public void replaceOptions(Map<ImmutableContextSet, Map<String, String>> map) {
        options.clear();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : map.entrySet()) {
            options.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, Boolean>> getAllPermissions() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, Boolean>> map = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : permissions.entrySet()) {
            map.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public CompletableFuture<Boolean> setPermission(ImmutableContextSet contexts, String permission, Tristate value) {
        boolean b;
        if (value == Tristate.UNDEFINED) {
            Map<String, Boolean> perms = permissions.get(contexts);
            b = perms != null && perms.remove(permission.toLowerCase()) != null;
        } else {
            Map<String, Boolean> perms = permissions.computeIfAbsent(contexts, c -> new ConcurrentHashMap<>());
            b = !Objects.equals(perms.put(permission.toLowerCase(), value.asBoolean()), value.asBoolean());
        }
        if (b) {
            permissionCache.invalidateAll();
            service.invalidateAllCaches(LPSubject.CacheLevel.PERMISSION);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        if (permissions.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        } else {
            permissions.clear();
            permissionCache.invalidateAll();
            service.invalidateAllCaches(LPSubject.CacheLevel.PERMISSION);
            return CompletableFuture.completedFuture(true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(ImmutableContextSet contexts) {
        Map<String, Boolean> perms = permissions.get(contexts);
        if (perms == null) {
            return CompletableFuture.completedFuture(false);
        }

        permissions.remove(contexts);
        if (!perms.isEmpty()) {
            permissionCache.invalidateAll();
            service.invalidateAllCaches(LPSubject.CacheLevel.PERMISSION);
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableList<SubjectReference>> getAllParents() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableList<SubjectReference>> map = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Set<SubjectReference>> e : parents.entrySet()) {
            map.put(e.getKey(), service.sortSubjects(e.getValue()));
        }
        return map.build();
    }

    @Override
    public CompletableFuture<Boolean> addParent(ImmutableContextSet contexts, SubjectReference parent) {
        Set<SubjectReference> set = parents.computeIfAbsent(contexts, c -> ConcurrentHashMap.newKeySet());
        boolean b = set.add(parent);
        if (b) {
            service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> removeParent(ImmutableContextSet contexts, SubjectReference parent) {
        Set<SubjectReference> set = parents.get(contexts);
        boolean b = set != null && set.remove(parent);
        if (b) {
            service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        if (parents.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        } else {
            parents.clear();
            service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
            return CompletableFuture.completedFuture(true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearParents(ImmutableContextSet contexts) {
        Set<SubjectReference> set = parents.get(contexts);
        if (set == null) {
            return CompletableFuture.completedFuture(false);
        }

        parents.remove(contexts);
        service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
        return CompletableFuture.completedFuture(!set.isEmpty());
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, String>> getAllOptions() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, String>> map = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : options.entrySet()) {
            map.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public CompletableFuture<Boolean> setOption(ImmutableContextSet contexts, String key, String value) {
        Map<String, String> options = this.options.computeIfAbsent(contexts, c -> new ConcurrentHashMap<>());
        boolean b = !stringEquals(options.put(key.toLowerCase(), value), value);
        if (b) {
            service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> unsetOption(ImmutableContextSet contexts, String key) {
        Map<String, String> options = this.options.get(contexts);
        boolean b = options != null && options.remove(key.toLowerCase()) != null;
        if (b) {
            service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        if (options.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        } else {
            options.clear();
            service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
            return CompletableFuture.completedFuture(true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(ImmutableContextSet contexts) {
        Map<String, String> map = options.get(contexts);
        if (map == null) {
            return CompletableFuture.completedFuture(false);
        }

        options.remove(contexts);
        service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
        return CompletableFuture.completedFuture(!map.isEmpty());
    }

    private static <V> Map<String, V> flattenMap(SortedMap<ImmutableContextSet, Map<String, V>> data) {
        Map<String, V> map = new HashMap<>();

        for (Map<String, V> m : data.values()) {
            for (Map.Entry<String, V> e : m.entrySet()) {
                map.putIfAbsent(e.getKey(), e.getValue());
            }
        }

        return ImmutableMap.copyOf(map);
    }

    private static <K, V> SortedMap<ImmutableContextSet, Map<K, V>> getRelevantEntries(ImmutableContextSet set, Map<ImmutableContextSet, Map<K, V>> map) {
        ImmutableSortedMap.Builder<ImmutableContextSet, Map<K, V>> perms = ImmutableSortedMap.orderedBy(ContextSetComparator.reverse());

        for (Map.Entry<ImmutableContextSet, Map<K, V>> e : map.entrySet()) {
            if (!e.getKey().isSatisfiedBy(set)) {
                continue;
            }

            perms.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }

        return perms.build();
    }

    private static boolean stringEquals(String a, String b) {
        return a == null && b == null || a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static class CalculatorHolder {

        @Getter
        private final PermissionCalculator calculator;

        @Getter
        private final Map<String, Boolean> permissions;

        public CalculatorHolder(PermissionCalculator calculator) {
            this.calculator = calculator;
            this.permissions = new ConcurrentHashMap<>();
            this.calculator.updateBacking(permissions);
        }

        public void setPermissions(Map<String, Boolean> permissions) {
            this.permissions.clear();
            this.permissions.putAll(permissions);
            calculator.updateBacking(this.permissions);
            calculator.invalidateCache();
        }
    }
}
