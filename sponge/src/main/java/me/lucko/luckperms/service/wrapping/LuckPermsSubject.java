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

package me.lucko.luckperms.service.wrapping;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.constants.Patterns;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.service.LuckPermsService;
import me.lucko.luckperms.users.User;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO
 * this class is aids rn
 */
@AllArgsConstructor
public class LuckPermsSubject implements Subject {
    private final EnduringData enduringData;
    private final LuckPermsService service;

    public LuckPermsSubject(PermissionHolder holder, LuckPermsService service) {
        this.enduringData = new EnduringData(service, holder);
        this.service = service;
    }

    @Override
    public String getIdentifier() {
        return enduringData.getHolder().getObjectName();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        if (enduringData.getHolder() instanceof User) {
            final UUID uuid = ((User) enduringData.getHolder()).getUuid();

            Optional<Player> p = Sponge.getServer().getPlayer(uuid);
            if (p.isPresent()) {
                return Optional.of(p.get());
            }
        }

        return Optional.empty();
    }

    @Override
    public SubjectCollection getContainingCollection() {
        if (enduringData.getHolder() instanceof Group) {
            return service.getGroupSubjects();
        } else {
            return service.getUserSubjects();
        }
    }

    @Override
    public SubjectData getSubjectData() {
        return null; // TODO
    }

    @Override
    public SubjectData getTransientSubjectData() {
        return null; // TODO
    }

    @Override
    public boolean hasPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        return getPermissionValue(contexts, node).asBoolean();
    }

    @Override
    public boolean hasPermission(String permission) {
        return getPermissionValue(getActiveContexts(), permission).asBoolean();
    }

    @Override
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String node) {
        return null;
        /*
        final Map<String, Boolean> nodes = applyContexts(contexts);

        if (nodes.containsKey(node)) {
            return Tristate.fromBoolean(nodes.get(node));
        } else {
            return Tristate.UNDEFINED;
        }
        */
    }

    @Override
    public boolean isChildOf(@NonNull Subject parent) {
        return isChildOf(getActiveContexts(), parent);
    }

    @Override
    public boolean isChildOf(@NonNull Set<Context> contexts, @NonNull Subject parent) {
        return parent instanceof PermissionHolder && getPermissionValue(contexts, "group." + parent.getIdentifier()).asBoolean();
    }

    @Override
    public List<Subject> getParents() {
        return getParents(getActiveContexts());
    }

    @Override
    public List<Subject> getParents(@NonNull Set<Context> contexts) {
        return null;
    }

    @Override
    public Optional<String> getOption(Set<Context> set, String s) {
        return null;
    }

    @Override
    public Optional<String> getOption(String key) {
        return null;
    }

    @Override
    public Set<Context> getActiveContexts() {
        return SubjectData.GLOBAL_CONTEXT;
    }

    @AllArgsConstructor
    public static class EnduringData implements SubjectData {
        private final LuckPermsService service;

        @Getter
        private final PermissionHolder holder;

        @Override
        public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
            return null;
            // TODO

            /*
            Map<String, Boolean> nodes = holder.convertTemporaryPerms();
            Map<Set<Context>, Map<String, Boolean>> permissions = new HashMap<>();

            for (Map.Entry<String, Boolean> e : nodes.entrySet()) {
                String node = e.getKey();
                if (node.contains("/")) {
                    // server and/or world specific
                    String[] parts = Patterns.SERVER_DELIMITER.split(node, 2);
                    // 0 = server+world 1 = node

                    node = parts[1];
                    String server = null;
                    String world = null;

                    if (parts[0].contains("-")) {
                        String[] serverParts = Patterns.WORLD_DELIMITER.split(parts[0], 2);
                        world = serverParts[0];
                        server = serverParts[1];

                    } else {
                        server = parts[0];
                    }

                    if (world == null) {
                        if (Patterns.NODE_CONTEXTS.matcher(node).matches()) {
                            // Has special context
                            Set<Context> c = Sets.newHashSet(new Context(LuckPermsService.SERVER_CONTEXT, server));

                            String[] contextParts = e.getKey().substring(1).split("\\)", 2);
                            // 0 = context, 1 = node

                            node = contextParts[1];

                            // Parse the context values from this node
                            for (String s : contextParts[0].split("\\,")) {
                                if (!s.contains("=")) {
                                    // Not valid
                                    continue;
                                }

                                // contextKey=value
                                String[] con = s.split("\\=", 2);
                                c.add(new Context(con[0], con[1]));
                            }

                            if (!permissions.containsKey(c)) {
                                permissions.put(c, new HashMap<>());
                            }
                            permissions.get(c).put(node, e.getValue());

                        } else {
                            // No special context
                            Set<Context> c = Sets.newHashSet(new Context(LuckPermsService.SERVER_CONTEXT, server));
                            if (!permissions.containsKey(c)) {
                                permissions.put(c, new HashMap<>());
                            }
                            permissions.get(c).put(node, e.getValue());
                        }
                    } else {
                        if (Patterns.NODE_CONTEXTS.matcher(node).matches()) {
                            // Has special context
                            Set<Context> c = Sets.newHashSet(new Context(Context.WORLD_KEY, world), new Context(LuckPermsService.SERVER_CONTEXT, server));

                            String[] contextParts = e.getKey().substring(1).split("\\)", 2);
                            // 0 = context, 1 = node

                            node = contextParts[1];

                            // Parse the context values from this node
                            for (String s : contextParts[0].split("\\,")) {
                                if (!s.contains("=")) {
                                    // Not valid
                                    continue;
                                }

                                // contextKey=value
                                String[] con = s.split("\\=", 2);
                                c.add(new Context(con[0], con[1]));
                            }

                            if (!permissions.containsKey(c)) {
                                permissions.put(c, new HashMap<>());
                            }
                            permissions.get(c).put(node, e.getValue());

                        } else {
                            // No special context
                            Set<Context> c = Sets.newHashSet(new Context(Context.WORLD_KEY, world), new Context(LuckPermsService.SERVER_CONTEXT, server));
                            if (!permissions.containsKey(c)) {
                                permissions.put(c, new HashMap<>());
                            }
                            permissions.get(c).put(node, e.getValue());
                        }
                    }

                } else {
                    // Plain node
                    if (Patterns.NODE_CONTEXTS.matcher(e.getKey()).matches()) {
                        // Has special context
                        Set<Context> c = Sets.newHashSet();

                        String[] contextParts = e.getKey().substring(1).split("\\)", 2);
                        // 0 = context, 1 = node

                        node = contextParts[1];

                        // Parse the context values from this node
                        for (String s : contextParts[0].split("\\,")) {
                            if (!s.contains("=")) {
                                // Not valid
                                continue;
                            }

                            // contextKey=value
                            String[] con = s.split("\\=", 2);
                            c.add(new Context(con[0], con[1]));
                        }

                        if (!permissions.containsKey(c)) {
                            permissions.put(c, new HashMap<>());
                        }
                        permissions.get(c).put(node, e.getValue());

                    } else {
                        if (!permissions.containsKey(new HashSet<Context>())) {
                            permissions.put(new HashSet<>(), new HashMap<>());
                        }
                        permissions.get(new HashSet<Context>()).put(node, e.getValue());
                    }
                }
            }

            return permissions;
            */
        }

        @Override
        public Map<String, Boolean> getPermissions(Set<Context> set) {
            return getAllPermissions().getOrDefault(set, Collections.emptyMap());
        }

        @Override
        public boolean setPermission(Set<Context> set, String s, Tristate tristate) {
            return false;
        }

        @Override
        public boolean clearPermissions() {
            return false;
        }

        @Override
        public boolean clearPermissions(Set<Context> set) {
            return false;
        }

        @Override
        public Map<Set<Context>, List<Subject>> getAllParents() {
            return null;
        }

        @Override
        public List<Subject> getParents(Set<Context> contexts) {
            final Set<String> parents = new HashSet<>();
            final Map<String, Boolean> nodes = applyContexts(contexts);

            for (Map.Entry<String, Boolean> e : nodes.entrySet()) {
                if (!e.getValue()) {
                    continue;
                }

                if (Patterns.GROUP_MATCH.matcher(e.getKey()).matches()) {
                    final String groupName = e.getKey().substring("group.".length());
                    parents.add(groupName);
                }
            }

            return parents.stream().map(s -> service.getGroupSubjects().get(s)).collect(Collectors.toList());
        }

        @Override
        public boolean addParent(Set<Context> set, Subject subject) {
            return false;
        }

        @Override
        public boolean removeParent(Set<Context> set, Subject subject) {
            return false;
        }

        @Override
        public boolean clearParents() {
            return false;
        }

        @Override
        public boolean clearParents(Set<Context> set) {
            return false;
        }

        @Override
        public Map<Set<Context>, Map<String, String>> getAllOptions() {
            return null;
        }

        @Override
        public Map<String, String> getOptions(Set<Context> set) {
            return null;
        }

        @Override
        public boolean setOption(Set<Context> set, String s, String s1) {
            return false;
        }

        @Override
        public boolean clearOptions(Set<Context> set) {
            return false;
        }

        @Override
        public boolean clearOptions() {
            return false;
        }

        private Map<String, Boolean> applyContexts(@NonNull Set<Context> set) {
            final Map<String, Boolean> map = new HashMap<>();

            String world = null;
            String server = null;

            Map<String, String> contexts = new HashMap<>();

            for (Context context : set) {
                if (context.getType().equals(Context.WORLD_KEY)) {
                    world = context.getName();
                    continue;
                }

                if (context.getType().equals(LuckPermsService.SERVER_CONTEXT)) {
                    server = context.getName();
                    continue;
                }

                contexts.put(context.getType(), context.getName());
            }

            Map<String, Boolean> local = holder.getLocalPermissions(server, world, null, service.getPossiblePermissions());
            perms:
            for (Map.Entry<String, Boolean> e : local.entrySet()) {
                if (!contexts.isEmpty()) {
                    if (!Patterns.NODE_CONTEXTS.matcher(e.getKey()).matches()) {
                        continue;
                    }

                    String[] parts = e.getKey().substring(1).split("\\)", 2);
                    // 0 = context, 1 = node

                    // Parse the context values from this node
                    Map<String, String> contextValues = new HashMap<>();
                    for (String s : parts[0].split("\\,")) {
                        if (!s.contains("=")) {
                            // Not valid
                            continue;
                        }

                        // contextKey=value
                        String[] con = s.split("\\=", 2);
                        contextValues.put(con[0], con[1]);
                    }

                    // Check that all of the requested contexts are met
                    for (Map.Entry<String, String> req : contexts.entrySet()) {
                        if (!contextValues.containsKey(e.getKey())) {
                            continue;
                        }

                        if (!contextValues.get(req.getKey()).equalsIgnoreCase(req.getValue())) {
                            // Not valid within the current contexts
                            continue perms;
                        }
                    }

                    // Passed all da tests.
                    map.put(parts[1], e.getValue());
                } else {
                    map.put(e.getKey(), e.getValue());
                }

            }

            return map;
        }
    }
}
