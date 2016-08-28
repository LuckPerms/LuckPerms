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

package me.lucko.luckperms.api.sponge;

import com.google.common.collect.ImmutableSet;
import lombok.*;
import me.lucko.luckperms.LPSpongePlugin;
import me.lucko.luckperms.api.sponge.collections.GroupCollection;
import me.lucko.luckperms.api.sponge.collections.UserCollection;
import me.lucko.luckperms.api.sponge.simple.SimpleCollection;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.*;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LuckPermsService implements PermissionService {
    public static final String SERVER_CONTEXT = "server";

    @Getter
    private final LPSpongePlugin plugin;

    @Getter
    private final UserCollection userSubjects;

    @Getter
    private final GroupCollection groupSubjects;

    @Getter
    private final Set<PermissionDescription> descriptionSet;

    private final Map<String, SubjectCollection> subjects;
    private final Set<ContextCalculator<Subject>> contextCalculators;

    public LuckPermsService(LPSpongePlugin plugin) {
        this.plugin = plugin;

        userSubjects = new UserCollection(this, plugin.getUserManager());
        groupSubjects = new GroupCollection(this, plugin.getGroupManager());

        subjects = new ConcurrentHashMap<>();
        subjects.put(PermissionService.SUBJECTS_USER, userSubjects);
        subjects.put(PermissionService.SUBJECTS_GROUP, groupSubjects);

        descriptionSet = ConcurrentHashMap.newKeySet();
        contextCalculators = ConcurrentHashMap.newKeySet();
    }

    public SubjectData getDefaultData() {
        return null; // TODO
    }

    @Override
    public Subject getDefaults() {
        return null; // TODO
    }

    @Override
    public SubjectCollection getSubjects(String s) {
        if (!subjects.containsKey(s)) {
            subjects.put(s, new SimpleCollection(this, s));
        }

        return subjects.get(s);
    }

    @Override
    public Map<String, SubjectCollection> getKnownSubjects() {
        return subjects;
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
        contextCalculators.add(contextCalculator);
    }

    public List<String> getPossiblePermissions() {
        return getDescriptions().stream().map(PermissionDescription::getId).collect(Collectors.toList());
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
