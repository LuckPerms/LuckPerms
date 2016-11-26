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

package me.lucko.luckperms.sponge.service.data;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.common.calculators.PermissionCalculator;
import me.lucko.luckperms.common.calculators.PermissionProcessor;
import me.lucko.luckperms.common.calculators.processors.MapProcessor;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.calculators.SpongeWildcardProcessor;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class CalculatedSubjectData implements SubjectData {
    private static final ContextComparator CONTEXT_COMPARATOR = new ContextComparator();

    private final LuckPermsService service;
    private final String calculatorDisplayName;

    private final LoadingCache<Set<Context>, CalculatorHolder> permissionCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<Set<Context>, CalculatorHolder>() {
                @Override
                public CalculatorHolder load(Set<Context> contexts) {
                    ImmutableList.Builder<PermissionProcessor> processors = ImmutableList.builder();
                    processors.add(new MapProcessor());
                    processors.add(new SpongeWildcardProcessor());

                    CalculatorHolder holder = new CalculatorHolder(new PermissionCalculator(service.getPlugin(), calculatorDisplayName, processors.build()));
                    holder.setPermissions(flattenMap(contexts, permissions));

                    return holder;
                }
            });

    private final LoadingCache<Set<Context>, Map<String, String>> optionCache = CacheBuilder.newBuilder()
            .build(new CacheLoader<Set<Context>, Map<String, String>>() {
                @Override
                public Map<String, String> load(Set<Context> contexts) {
                    return flattenMap(contexts, options);
                }
            });

    private final Map<Set<Context>, Map<String, Boolean>> permissions = new ConcurrentHashMap<>();
    private final Map<Set<Context>, Set<SubjectReference>> parents = new ConcurrentHashMap<>();
    private final Map<Set<Context>, Map<String, String>> options = new ConcurrentHashMap<>();

    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        return LuckPermsService.convertTristate(permissionCache.getUnchecked(contexts).getCalculator().getPermissionValue(permission));
    }

    public Map<Set<Context>, Set<SubjectReference>> getParents() {
        ImmutableMap.Builder<Set<Context>, Set<SubjectReference>> map = ImmutableMap.builder();
        for (Map.Entry<Set<Context>, Set<SubjectReference>> e : parents.entrySet()) {
            map.put(ImmutableSet.copyOf(e.getKey()), ImmutableSet.copyOf(e.getValue()));
        }
        return map.build();
    }

    public void replacePermissions(Map<Set<Context>, Map<String, Boolean>> map) {
        permissions.clear();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> e : map.entrySet()) {
            permissions.put(ImmutableSet.copyOf(e.getKey()), new ConcurrentHashMap<>(e.getValue()));
        }
        permissionCache.invalidateAll();
    }

    public void replaceParents(Map<Set<Context>, Set<SubjectReference>> map) {
        parents.clear();
        for (Map.Entry<Set<Context>, Set<SubjectReference>> e : map.entrySet()) {
            Set<SubjectReference> set = ConcurrentHashMap.newKeySet();
            set.addAll(e.getValue());
            parents.put(ImmutableSet.copyOf(e.getKey()), set);
        }
    }

    public void replaceOptions(Map<Set<Context>, Map<String, String>> map) {
        options.clear();
        for (Map.Entry<Set<Context>, Map<String, String>> e : map.entrySet()) {
            options.put(ImmutableSet.copyOf(e.getKey()), new ConcurrentHashMap<>(e.getValue()));
        }
        optionCache.invalidateAll();
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        ImmutableMap.Builder<Set<Context>, Map<String, Boolean>> map = ImmutableMap.builder();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> e : permissions.entrySet()) {
            map.put(ImmutableSet.copyOf(e.getKey()), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> contexts) {
        return ImmutableMap.copyOf(permissions.getOrDefault(contexts, ImmutableMap.of()));
    }

    @Override
    public boolean setPermission(Set<Context> contexts, String permission, Tristate value) {
        boolean b;
        if (value == Tristate.UNDEFINED) {
            Map<String, Boolean> perms = permissions.get(contexts);
            b = perms != null && perms.remove(permission.toLowerCase()) != null;
        } else {
            Map<String, Boolean> perms = permissions.computeIfAbsent(ImmutableSet.copyOf(contexts), c -> new ConcurrentHashMap<>());
            b = !Objects.equals(perms.put(permission.toLowerCase(), value.asBoolean()), value.asBoolean());
        }
        if (b) {
            permissionCache.invalidateAll();
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
            return true;
        }
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts) {
        Map<String, Boolean> perms = permissions.get(contexts);
        if (perms == null) {
            return false;
        }

        permissions.remove(contexts);
        if (!perms.isEmpty()) {
            permissionCache.invalidateAll();
            return true;
        }
        return false;
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        ImmutableMap.Builder<Set<Context>, List<Subject>> map = ImmutableMap.builder();
        for (Map.Entry<Set<Context>, Set<SubjectReference>> e : parents.entrySet()) {
            map.put(
                    ImmutableSet.copyOf(e.getKey()),
                    e.getValue().stream().map(s -> s.resolve(service)).collect(ImmutableCollectors.toImmutableList())
            );
        }
        return map.build();
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts) {
        return parents.getOrDefault(contexts, ImmutableSet.of()).stream()
                .map(s -> s.resolve(service))
                .collect(ImmutableCollectors.toImmutableList());
    }

    @Override
    public boolean addParent(Set<Context> contexts, Subject parent) {
        Set<SubjectReference> set = parents.computeIfAbsent(ImmutableSet.copyOf(contexts), c -> ConcurrentHashMap.newKeySet());
        return set.add(SubjectReference.of(parent));
    }

    @Override
    public boolean removeParent(Set<Context> contexts, Subject parent) {
        Set<SubjectReference> set = parents.get(contexts);
        return set != null && set.remove(SubjectReference.of(parent));
    }

    @Override
    public boolean clearParents() {
        if (parents.isEmpty()) {
            return false;
        } else {
            parents.clear();
            return true;
        }
    }

    @Override
    public boolean clearParents(Set<Context> contexts) {
        Set<SubjectReference> set = parents.get(contexts);
        if (set == null) {
            return false;
        }

        parents.remove(contexts);
        return !set.isEmpty();
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        ImmutableMap.Builder<Set<Context>, Map<String, String>> map = ImmutableMap.builder();
        for (Map.Entry<Set<Context>, Map<String, String>> e : options.entrySet()) {
            map.put(ImmutableSet.copyOf(e.getKey()), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {
        return ImmutableMap.copyOf(options.getOrDefault(contexts, ImmutableMap.of()));
    }

    @Override
    public boolean setOption(Set<Context> contexts, String key, @Nullable String value) {
        boolean b;
        if (value == null) {
            Map<String, String> options = this.options.get(contexts);
            b = options != null && options.remove(key.toLowerCase()) != null;
        } else {
            Map<String, String> options = this.options.computeIfAbsent(ImmutableSet.copyOf(contexts), c -> new ConcurrentHashMap<>());
            b = !stringEquals(options.put(key.toLowerCase(), value), value);
        }
        if (b) {
            optionCache.invalidateAll();
        }
        return b;
    }

    @Override
    public boolean clearOptions() {
        if (options.isEmpty()) {
            return false;
        } else {
            options.clear();
            optionCache.invalidateAll();
            return true;
        }
    }

    @Override
    public boolean clearOptions(Set<Context> contexts) {
        Map<String, String> map = options.get(contexts);
        if (map == null) {
            return false;
        }

        options.remove(contexts);
        if (!map.isEmpty()) {
            optionCache.invalidateAll();
            return true;
        }
        return false;
    }

    private static <V> Map<String, V> flattenMap(Set<Context> contexts, Map<Set<Context>, Map<String, V>> source) {
        Map<String, V> map = new HashMap<>();

        SortedMap<Set<Context>, Map<String, V>> ret = getRelevantEntries(contexts, source);
        for (Map<String, V> m : ret.values()) {
            for (Map.Entry<String, V> e : m.entrySet()) {
                if (!map.containsKey(e.getKey())) {
                    map.put(e.getKey(), e.getValue());
                }
            }
        }

        return ImmutableMap.copyOf(map);
    }

    private static <K, V> SortedMap<Set<Context>, Map<K, V>> getRelevantEntries(Set<Context> set, Map<Set<Context>, Map<K, V>> map) {
        ImmutableSortedMap.Builder<Set<Context>, Map<K, V>> perms = ImmutableSortedMap.orderedBy(CONTEXT_COMPARATOR);

        loop:
        for (Map.Entry<Set<Context>, Map<K, V>> e : map.entrySet()) {
            for (Context c : e.getKey()) {
                if (!set.contains(c)) {
                    continue loop;
                }
            }

            perms.put(ImmutableSet.copyOf(e.getKey()), ImmutableMap.copyOf(e.getValue()));
        }

        return perms.build();
    }

    private static boolean stringEquals(String a, String b) {
        return a == null && b == null || a != null && b != null && a.equalsIgnoreCase(b);
    }

    private static class ContextComparator implements Comparator<Set<Context>> {

        @Override
        public int compare(Set<Context> o1, Set<Context> o2) {
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
