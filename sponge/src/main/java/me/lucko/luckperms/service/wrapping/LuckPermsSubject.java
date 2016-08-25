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

import lombok.AllArgsConstructor;
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

@AllArgsConstructor
public class LuckPermsSubject implements Subject {
    private final PermissionHolder holder;
    private final LuckPermsService service;

    @Override
    public String getIdentifier() {
        return holder.getObjectName();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        if (holder instanceof User) {
            final UUID uuid = ((User) holder).getUuid();

            Optional<Player> p = Sponge.getServer().getPlayer(uuid);
            if (p.isPresent()) {
                return Optional.of(p.get());
            }
        }

        return Optional.empty();
    }

    @Override
    public SubjectCollection getContainingCollection() {
        if (holder instanceof Group) {
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

    @Override
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String node) {
        final Map<String, Boolean> nodes = applyContexts(contexts);

        if (nodes.containsKey(node)) {
            return Tristate.fromBoolean(nodes.get(node));
        } else {
            return Tristate.UNDEFINED;
        }
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
    public Set<Context> getActiveContexts() {
        return SubjectData.GLOBAL_CONTEXT;
    }
}
