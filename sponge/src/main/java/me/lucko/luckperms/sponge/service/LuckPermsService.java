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

package me.lucko.luckperms.sponge.service;

import co.aikar.timings.Timing;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import lombok.*;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.contexts.SpongeCalculatorLink;
import me.lucko.luckperms.sponge.service.collections.GroupCollection;
import me.lucko.luckperms.sponge.service.collections.UserCollection;
import me.lucko.luckperms.sponge.service.persisted.PersistedCollection;
import me.lucko.luckperms.sponge.service.persisted.SubjectStorage;
import me.lucko.luckperms.sponge.service.simple.SimpleCollection;
import me.lucko.luckperms.sponge.timings.LPTiming;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.*;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The LuckPerms implementation of the Sponge Permission Service
 */
@Getter
public class LuckPermsService implements PermissionService {
    public static final String SERVER_CONTEXT = "server";

    private final LPSpongePlugin plugin;
    private final SubjectStorage storage;
    private final UserCollection userSubjects;
    private final GroupCollection groupSubjects;
    private final PersistedCollection defaultSubjects;
    private final Set<PermissionDescription> descriptionSet;

    @Getter(value = AccessLevel.NONE)
    private final LoadingCache<String, SubjectCollection> collections = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, SubjectCollection>() {
                @Override
                public SubjectCollection load(String s) {
                    return new SimpleCollection(LuckPermsService.this, s);
                }
            });

    public LuckPermsService(LPSpongePlugin plugin) {
        this.plugin = plugin;

        storage = new SubjectStorage(new File(plugin.getDataFolder(), "local"));

        userSubjects = new UserCollection(this, plugin.getUserManager());
        groupSubjects = new GroupCollection(this, plugin.getGroupManager());
        defaultSubjects = new PersistedCollection(this, "defaults");
        defaultSubjects.loadAll();

        collections.put(PermissionService.SUBJECTS_USER, userSubjects);
        collections.put(PermissionService.SUBJECTS_GROUP, groupSubjects);
        collections.put("defaults", defaultSubjects);

        descriptionSet = ConcurrentHashMap.newKeySet();
    }

    public SubjectData getDefaultData() {
        return getDefaults().getSubjectData();
    }

    @Override
    public Subject getDefaults() {
        return getDefaultSubjects().get("default");
    }

    @Override
    public SubjectCollection getSubjects(String s) {
        try (Timing ignored = plugin.getTimings().time(LPTiming.GET_SUBJECTS)) {
            return collections.getUnchecked(s.toLowerCase());
        }
    }

    @Override
    public Map<String, SubjectCollection> getKnownSubjects() {
        return ImmutableMap.copyOf(collections.asMap());
    }

    @Override
    public Optional<PermissionDescription.Builder> newDescriptionBuilder(@NonNull Object o) {
        Optional<PluginContainer> container = plugin.getGame().getPluginManager().fromInstance(o);
        if (!container.isPresent()) {
            throw new IllegalArgumentException("Couldn't find a plugin container for " + o.getClass().getSimpleName());
        }

        return Optional.of(new DescriptionBuilder(this, container.get()));
    }

    @Override
    public Optional<PermissionDescription> getDescription(@NonNull String s) {
        for (PermissionDescription d : descriptionSet) {
            if (d.getId().equals(s)) {
                return Optional.of(d);
            }
        }

        return Optional.empty();
    }

    @Override
    public Collection<PermissionDescription> getDescriptions() {
        return ImmutableSet.copyOf(descriptionSet);
    }

    @Override
    public void registerContextCalculator(@NonNull ContextCalculator<Subject> contextCalculator) {
        plugin.getContextManager().registerCalculator(new SpongeCalculatorLink(contextCalculator));
    }

    public Contexts calculateContexts(ContextSet contextSet) {
        return new Contexts(
                contextSet,
                plugin.getConfiguration().isIncludingGlobalPerms(),
                plugin.getConfiguration().isIncludingGlobalWorldPerms(),
                true,
                plugin.getConfiguration().isApplyingGlobalGroups(),
                plugin.getConfiguration().isApplyingGlobalWorldGroups(),
                false
        );
    }

    public static ContextSet convertContexts(Set<Context> contexts) {
        return ContextSet.fromEntries(contexts.stream().map(c -> Maps.immutableEntry(c.getKey(), c.getValue())).collect(Collectors.toSet()));
    }

    public static Set<Context> convertContexts(ContextSet contexts) {
        return contexts.toSet().stream().map(e -> new Context(e.getKey(), e.getValue())).collect(Collectors.toSet());
    }

    public static Tristate convertTristate(me.lucko.luckperms.api.Tristate tristate) {
        switch (tristate) {
            case TRUE:
                return Tristate.TRUE;
            case FALSE:
                return Tristate.FALSE;
            default:
                return Tristate.UNDEFINED;
        }
    }

    public static me.lucko.luckperms.api.Tristate convertTristate(Tristate tristate) {
        switch (tristate) {
            case TRUE:
                return me.lucko.luckperms.api.Tristate.TRUE;
            case FALSE:
                return me.lucko.luckperms.api.Tristate.FALSE;
            default:
                return me.lucko.luckperms.api.Tristate.UNDEFINED;
        }
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class DescriptionBuilder implements PermissionDescription.Builder {
        private final LuckPermsService service;
        private final PluginContainer container;

        private String id = null;
        private Text description = null;
        private final Map<String, Tristate> roles = new HashMap<>();

        @Override
        public PermissionDescription.Builder id(@NonNull String s) {
            id = s;
            return this;
        }

        @Override
        public PermissionDescription.Builder description(@NonNull Text text) {
            description = text;
            return this;
        }

        @Override
        public PermissionDescription.Builder assign(@NonNull String s, boolean b) {
            roles.put(s, Tristate.fromBoolean(b));
            return this;
        }

        @Override
        public PermissionDescription register() throws IllegalStateException {
            if (id == null) {
                throw new IllegalStateException("id cannot be null");
            }
            if (description == null) {
                throw new IllegalStateException("description cannot be null");
            }

            Description d = new Description(service, container, id, description);
            service.getDescriptionSet().add(d);

            // Set role-templates
            SubjectCollection subjects = service.getSubjects(PermissionService.SUBJECTS_ROLE_TEMPLATE);
            for (Map.Entry<String, Tristate> assignment : roles.entrySet()) {
                Subject subject = subjects.get(assignment.getKey());
                subject.getTransientSubjectData().setPermission(SubjectData.GLOBAL_CONTEXT, id, assignment.getValue());
            }

            service.getPlugin().getPermissionCache().offer(id);

            return d;
        }
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static class Description implements PermissionDescription {
        private final LuckPermsService service;
        private final PluginContainer owner;
        private final String id;
        private final Text description;

        @Override
        public Map<Subject, Boolean> getAssignedSubjects(String id) {
            SubjectCollection subjects = service.getSubjects(id);
            return subjects.getAllWithPermission(this.id);
        }
    }
}
