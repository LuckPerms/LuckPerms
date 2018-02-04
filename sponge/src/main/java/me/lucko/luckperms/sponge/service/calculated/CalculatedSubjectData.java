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
import me.lucko.luckperms.sponge.service.reference.LPSubjectReference;

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
public class CalculatedSubjectData implements LPSubjectData {

    private final LPSubject parentSubject;

    private final LPPermissionService service;
    private final String calculatorDisplayName;

    private final Map<ImmutableContextSet, Map<String, Boolean>> permissions = new ConcurrentHashMap<>();
    private final Map<ImmutableContextSet, Set<LPSubjectReference>> parents = new ConcurrentHashMap<>();
    private final Map<ImmutableContextSet, Map<String, String>> options = new ConcurrentHashMap<>();

    private final LoadingCache<ImmutableContextSet, CalculatorHolder> permissionCache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(contexts -> {
                ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();
                processors.add(new MapProcessor());
                processors.add(new SpongeWildcardProcessor());

                CalculatorHolder holder = new CalculatorHolder(new PermissionCalculator(CalculatedSubjectData.this.service.getPlugin(), PermissionCalculatorMetadata.of(HolderType.GROUP, CalculatedSubjectData.this.calculatorDisplayName, contexts), processors.build()));
                holder.setPermissions(flattenMap(getRelevantEntries(contexts, CalculatedSubjectData.this.permissions)));

                return holder;
            });

    public CalculatedSubjectData(LPSubject parentSubject, LPPermissionService service, String calculatorDisplayName) {
        this.parentSubject = parentSubject;
        this.service = service;
        this.calculatorDisplayName = calculatorDisplayName;
    }

    @Override
    public LPSubject getParentSubject() {
        return this.parentSubject;
    }

    public void cleanup() {
        this.permissionCache.cleanUp();
    }

    public void invalidateLookupCache() {
        this.permissionCache.invalidateAll();
    }

    public Tristate getPermissionValue(ImmutableContextSet contexts, String permission) {
        return this.permissionCache.get(contexts).getCalculator().getPermissionValue(permission, CheckOrigin.INTERNAL);
    }

    public void replacePermissions(Map<ImmutableContextSet, Map<String, Boolean>> map) {
        this.permissions.clear();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : map.entrySet()) {
            this.permissions.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        this.permissionCache.invalidateAll();
        this.service.invalidateAllCaches(LPSubject.CacheLevel.PERMISSION);
    }

    public void replaceParents(Map<ImmutableContextSet, List<LPSubjectReference>> map) {
        this.parents.clear();
        for (Map.Entry<ImmutableContextSet, List<LPSubjectReference>> e : map.entrySet()) {
            Set<LPSubjectReference> set = ConcurrentHashMap.newKeySet();
            set.addAll(e.getValue());
            this.parents.put(e.getKey(), set);
        }
        this.service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
    }

    public void replaceOptions(Map<ImmutableContextSet, Map<String, String>> map) {
        this.options.clear();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : map.entrySet()) {
            this.options.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        this.service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, Boolean>> getAllPermissions() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, Boolean>> map = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : this.permissions.entrySet()) {
            map.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public CompletableFuture<Boolean> setPermission(ImmutableContextSet contexts, String permission, Tristate value) {
        boolean b;
        if (value == Tristate.UNDEFINED) {
            Map<String, Boolean> perms = this.permissions.get(contexts);
            b = perms != null && perms.remove(permission.toLowerCase()) != null;
        } else {
            Map<String, Boolean> perms = this.permissions.computeIfAbsent(contexts, c -> new ConcurrentHashMap<>());
            b = !Objects.equals(perms.put(permission.toLowerCase(), value.asBoolean()), value.asBoolean());
        }
        if (b) {
            this.permissionCache.invalidateAll();
            this.service.invalidateAllCaches(LPSubject.CacheLevel.PERMISSION);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions() {
        if (this.permissions.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        } else {
            this.permissions.clear();
            this.permissionCache.invalidateAll();
            this.service.invalidateAllCaches(LPSubject.CacheLevel.PERMISSION);
            return CompletableFuture.completedFuture(true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearPermissions(ImmutableContextSet contexts) {
        Map<String, Boolean> perms = this.permissions.get(contexts);
        if (perms == null) {
            return CompletableFuture.completedFuture(false);
        }

        this.permissions.remove(contexts);
        if (!perms.isEmpty()) {
            this.permissionCache.invalidateAll();
            this.service.invalidateAllCaches(LPSubject.CacheLevel.PERMISSION);
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableList<LPSubjectReference>> getAllParents() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableList<LPSubjectReference>> map = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Set<LPSubjectReference>> e : this.parents.entrySet()) {
            map.put(e.getKey(), this.service.sortSubjects(e.getValue()));
        }
        return map.build();
    }

    @Override
    public CompletableFuture<Boolean> addParent(ImmutableContextSet contexts, LPSubjectReference parent) {
        Set<LPSubjectReference> set = this.parents.computeIfAbsent(contexts, c -> ConcurrentHashMap.newKeySet());
        boolean b = set.add(parent);
        if (b) {
            this.service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> removeParent(ImmutableContextSet contexts, LPSubjectReference parent) {
        Set<LPSubjectReference> set = this.parents.get(contexts);
        boolean b = set != null && set.remove(parent);
        if (b) {
            this.service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> clearParents() {
        if (this.parents.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        } else {
            this.parents.clear();
            this.service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
            return CompletableFuture.completedFuture(true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearParents(ImmutableContextSet contexts) {
        Set<LPSubjectReference> set = this.parents.get(contexts);
        if (set == null) {
            return CompletableFuture.completedFuture(false);
        }

        this.parents.remove(contexts);
        this.service.invalidateAllCaches(LPSubject.CacheLevel.PARENT);
        return CompletableFuture.completedFuture(!set.isEmpty());
    }

    @Override
    public ImmutableMap<ImmutableContextSet, ImmutableMap<String, String>> getAllOptions() {
        ImmutableMap.Builder<ImmutableContextSet, ImmutableMap<String, String>> map = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : this.options.entrySet()) {
            map.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public CompletableFuture<Boolean> setOption(ImmutableContextSet contexts, String key, String value) {
        Map<String, String> options = this.options.computeIfAbsent(contexts, c -> new ConcurrentHashMap<>());
        boolean b = !stringEquals(options.put(key.toLowerCase(), value), value);
        if (b) {
            this.service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> unsetOption(ImmutableContextSet contexts, String key) {
        Map<String, String> options = this.options.get(contexts);
        boolean b = options != null && options.remove(key.toLowerCase()) != null;
        if (b) {
            this.service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
        }
        return CompletableFuture.completedFuture(b);
    }

    @Override
    public CompletableFuture<Boolean> clearOptions() {
        if (this.options.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        } else {
            this.options.clear();
            this.service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
            return CompletableFuture.completedFuture(true);
        }
    }

    @Override
    public CompletableFuture<Boolean> clearOptions(ImmutableContextSet contexts) {
        Map<String, String> map = this.options.get(contexts);
        if (map == null) {
            return CompletableFuture.completedFuture(false);
        }

        this.options.remove(contexts);
        this.service.invalidateAllCaches(LPSubject.CacheLevel.OPTION);
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

        private final PermissionCalculator calculator;

        private final Map<String, Boolean> permissions;

        public CalculatorHolder(PermissionCalculator calculator) {
            this.calculator = calculator;
            this.permissions = new ConcurrentHashMap<>();
            this.calculator.setSourcePermissions(this.permissions);
        }

        public void setPermissions(Map<String, Boolean> permissions) {
            this.permissions.clear();
            this.permissions.putAll(permissions);
            this.calculator.setSourcePermissions(this.permissions);
            this.calculator.invalidateCache();
        }

        public PermissionCalculator getCalculator() {
            return this.calculator;
        }
    }
}
