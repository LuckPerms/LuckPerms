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

package me.lucko.luckperms.sponge.service.calculated;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionProcessor;
import me.lucko.luckperms.common.calculators.processors.MapProcessor;
import me.lucko.luckperms.sponge.calculators.SpongeWildcardProcessor;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.proxy.LPSubject;
import me.lucko.luckperms.sponge.service.proxy.LPSubjectData;
import me.lucko.luckperms.sponge.service.references.SubjectReference;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class CalculatedSubjectData implements LPSubjectData {
    private static final ContextComparator CONTEXT_COMPARATOR = new ContextComparator();

    @Getter
    private final LPSubject parentSubject;

    private final LuckPermsService service;
    private final String calculatorDisplayName;

    private final Map<ContextSet, Map<String, Boolean>> permissions = new ConcurrentHashMap<>();
    private final LoadingCache<ContextSet, CalculatorHolder> permissionCache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<ContextSet, CalculatorHolder>() {
                @Override
                public CalculatorHolder load(ContextSet contexts) {
                    ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();
                    processors.add(new MapProcessor());
                    processors.add(new SpongeWildcardProcessor());

                    CalculatorHolder holder = new CalculatorHolder(new PermissionCalculator(service.getPlugin(), calculatorDisplayName, processors.build()));
                    holder.setPermissions(flattenMap(contexts, permissions));

                    return holder;
                }
            });

    private final Map<ContextSet, Set<SubjectReference>> parents = new ConcurrentHashMap<>();
    private final Map<ContextSet, Map<String, String>> options = new ConcurrentHashMap<>();

    public void cleanup() {
        permissionCache.cleanUp();
    }

    public void invalidateLookupCache() {
        permissionCache.invalidateAll();
    }

    public Tristate getPermissionValue(ContextSet contexts, String permission) {
        return permissionCache.get(contexts).getCalculator().getPermissionValue(permission);
    }

    public void replacePermissions(Map<ImmutableContextSet, Map<String, Boolean>> map) {
        permissions.clear();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : map.entrySet()) {
            permissions.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        permissionCache.invalidateAll();
        service.invalidatePermissionCaches();
    }

    public void replaceParents(Map<ImmutableContextSet, List<SubjectReference>> map) {
        parents.clear();
        for (Map.Entry<ImmutableContextSet, List<SubjectReference>> e : map.entrySet()) {
            Set<SubjectReference> set = ConcurrentHashMap.newKeySet();
            set.addAll(e.getValue());
            parents.put(e.getKey(), set);
        }
        service.invalidateParentCaches();
    }

    public void replaceOptions(Map<ImmutableContextSet, Map<String, String>> map) {
        options.clear();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : map.entrySet()) {
            options.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
        }
        service.invalidateOptionCaches();
    }

    @Override
    public Map<ImmutableContextSet, Map<String, Boolean>> getPermissions() {
        ImmutableMap.Builder<ImmutableContextSet, Map<String, Boolean>> map = ImmutableMap.builder();
        for (Map.Entry<ContextSet, Map<String, Boolean>> e : permissions.entrySet()) {
            map.put(e.getKey().makeImmutable(), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public Map<String, Boolean> getPermissions(ContextSet contexts) {
        return ImmutableMap.copyOf(permissions.getOrDefault(contexts, ImmutableMap.of()));
    }

    @Override
    public boolean setPermission(ContextSet contexts, String permission, Tristate value) {
        boolean b;
        if (value == Tristate.UNDEFINED) {
            Map<String, Boolean> perms = permissions.get(contexts);
            b = perms != null && perms.remove(permission.toLowerCase()) != null;
        } else {
            Map<String, Boolean> perms = permissions.computeIfAbsent(contexts.makeImmutable(), c -> new ConcurrentHashMap<>());
            b = !Objects.equals(perms.put(permission.toLowerCase(), value.asBoolean()), value.asBoolean());
        }
        if (b) {
            permissionCache.invalidateAll();
            service.invalidatePermissionCaches();
        }
        return b;
    }

    @Override
    public boolean clearPermissions() {
        if (permissions.isEmpty()) {
            return false;
        } else {
            permissions.clear();
            permissionCache.invalidateAll();
            service.invalidatePermissionCaches();
            return true;
        }
    }

    @Override
    public boolean clearPermissions(ContextSet contexts) {
        Map<String, Boolean> perms = permissions.get(contexts);
        if (perms == null) {
            return false;
        }

        permissions.remove(contexts);
        if (!perms.isEmpty()) {
            permissionCache.invalidateAll();
            service.invalidatePermissionCaches();
            return true;
        }
        return false;
    }

    @Override
    public Map<ImmutableContextSet, Set<SubjectReference>> getParents() {
        ImmutableMap.Builder<ImmutableContextSet, Set<SubjectReference>> map = ImmutableMap.builder();
        for (Map.Entry<ContextSet, Set<SubjectReference>> e : parents.entrySet()) {
            map.put(e.getKey().makeImmutable(), ImmutableSet.copyOf(e.getValue()));
        }
        return map.build();
    }

    public Map<ImmutableContextSet, List<SubjectReference>> getParentsAsList() {
        ImmutableMap.Builder<ImmutableContextSet, List<SubjectReference>> map = ImmutableMap.builder();
        for (Map.Entry<ContextSet, Set<SubjectReference>> e : parents.entrySet()) {
            map.put(e.getKey().makeImmutable(), ImmutableList.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public Set<SubjectReference> getParents(ContextSet contexts) {
        return ImmutableSet.copyOf(parents.getOrDefault(contexts, ImmutableSet.of()));
    }

    @Override
    public boolean addParent(ContextSet contexts, SubjectReference parent) {
        Set<SubjectReference> set = parents.computeIfAbsent(contexts.makeImmutable(), c -> ConcurrentHashMap.newKeySet());
        boolean b = set.add(parent);
        if (b) {
            service.invalidateParentCaches();
        }
        return b;
    }

    @Override
    public boolean removeParent(ContextSet contexts, SubjectReference parent) {
        Set<SubjectReference> set = parents.get(contexts);
        boolean b = set != null && set.remove(parent);
        if (b) {
            service.invalidateParentCaches();
        }
        return b;
    }

    @Override
    public boolean clearParents() {
        if (parents.isEmpty()) {
            return false;
        } else {
            parents.clear();
            service.invalidateOptionCaches();
            return true;
        }
    }

    @Override
    public boolean clearParents(ContextSet contexts) {
        Set<SubjectReference> set = parents.get(contexts);
        if (set == null) {
            return false;
        }

        parents.remove(contexts);
        service.invalidateParentCaches();
        return !set.isEmpty();
    }

    @Override
    public Map<ImmutableContextSet, Map<String, String>> getOptions() {
        ImmutableMap.Builder<ImmutableContextSet, Map<String, String>> map = ImmutableMap.builder();
        for (Map.Entry<ContextSet, Map<String, String>> e : options.entrySet()) {
            map.put(e.getKey().makeImmutable(), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public Map<String, String> getOptions(ContextSet contexts) {
        return ImmutableMap.copyOf(options.getOrDefault(contexts, ImmutableMap.of()));
    }

    @Override
    public boolean setOption(ContextSet contexts, String key, String value) {
        Map<String, String> options = this.options.computeIfAbsent(contexts.makeImmutable(), c -> new ConcurrentHashMap<>());
        boolean b = !stringEquals(options.put(key.toLowerCase(), value), value);
        if (b) {
            service.invalidateOptionCaches();
        }
        return b;
    }

    @Override
    public boolean unsetOption(ContextSet contexts, String key) {
        Map<String, String> options = this.options.get(contexts);
        boolean b = options != null && options.remove(key.toLowerCase()) != null;
        if (b) {
            service.invalidateOptionCaches();
        }
        return b;
    }

    @Override
    public boolean clearOptions() {
        if (options.isEmpty()) {
            return false;
        } else {
            options.clear();
            service.invalidateOptionCaches();
            return true;
        }
    }

    @Override
    public boolean clearOptions(ContextSet contexts) {
        Map<String, String> map = options.get(contexts);
        if (map == null) {
            return false;
        }

        options.remove(contexts);
        service.invalidateOptionCaches();
        return !map.isEmpty();
    }

    private static <V> Map<String, V> flattenMap(ContextSet contexts, Map<ContextSet, Map<String, V>> source) {
        Map<String, V> map = new HashMap<>();

        SortedMap<ContextSet, Map<String, V>> ret = getRelevantEntries(contexts, source);
        for (Map<String, V> m : ret.values()) {
            for (Map.Entry<String, V> e : m.entrySet()) {
                if (!map.containsKey(e.getKey())) {
                    map.put(e.getKey(), e.getValue());
                }
            }
        }

        return ImmutableMap.copyOf(map);
    }

    private static <K, V> SortedMap<ContextSet, Map<K, V>> getRelevantEntries(ContextSet set, Map<ContextSet, Map<K, V>> map) {
        ImmutableSortedMap.Builder<ContextSet, Map<K, V>> perms = ImmutableSortedMap.orderedBy(CONTEXT_COMPARATOR);

        for (Map.Entry<ContextSet, Map<K, V>> e : map.entrySet()) {
            if (!e.getKey().isSatisfiedBy(set)) {
                continue;
            }

            perms.put(e.getKey().makeImmutable(), ImmutableMap.copyOf(e.getValue()));
        }

        return perms.build();
    }

    private static boolean stringEquals(String a, String b) {
        return a == null && b == null || a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static class ContextComparator implements Comparator<ContextSet> {

        @Override
        public int compare(ContextSet o1, ContextSet o2) {
            int i = Integer.compare(o1.size(), o2.size());
            return i == 0 ? 1 : i;
        }
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
