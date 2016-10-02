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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.users.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.stream.Collectors;

import static me.lucko.luckperms.utils.ArgumentChecker.escapeCharacters;
import static me.lucko.luckperms.utils.ArgumentChecker.unescapeCharacters;

@AllArgsConstructor
public class LuckPermsSubjectData implements SubjectData {
    private final boolean enduring;
    private final LuckPermsSubject superClass;
    private final LuckPermsService service;

    @Getter
    private final PermissionHolder holder;

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        Map<Set<Context>, Map<String, Boolean>> perms = new HashMap<>();

        for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
            Set<Context> contexts = n.getExtraContexts().entrySet().stream()
                    .map(entry -> new Context(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet());

            if (n.isServerSpecific()) {
                contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
            }

            if (n.isWorldSpecific()) {
                contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
            }

            if (!perms.containsKey(contexts)) {
                perms.put(contexts, new HashMap<>());
            }

            perms.get(contexts).put(n.getPermission(), n.getValue());
        }

        ImmutableMap.Builder<Set<Context>, Map<String, Boolean>> map = ImmutableMap.builder();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> e : perms.entrySet()) {
            map.put(ImmutableSet.copyOf(e.getKey()), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> set) {
        return getAllPermissions().getOrDefault(set, ImmutableMap.of());
    }

    @Override
    public boolean setPermission(Set<Context> set, String s, Tristate tristate) {
        if (tristate == Tristate.UNDEFINED) {
            // Unset
            Node.Builder builder = new me.lucko.luckperms.core.Node.Builder(s);

            for (Context ct : set) {
                builder.withExtraContext(ct.getKey(), ct.getValue());
            }

            try {
                if (enduring) {
                    holder.unsetPermission(builder.build());
                } else {
                    holder.unsetTransientPermission(builder.build());
                }
            } catch (ObjectLacksException ignored) {}
            superClass.objectSave(holder);
            return true;
        }

        Node.Builder builder = new me.lucko.luckperms.core.Node.Builder(s)
                .setValue(tristate.asBoolean());

        for (Context ct : set) {
            builder.withExtraContext(ct.getKey(), ct.getValue());
        }

        try {
            if (enduring) {
                holder.setPermission(builder.build());
            } else {
                holder.setTransientPermission(builder.build());
            }
        } catch (ObjectAlreadyHasException ignored) {}
        superClass.objectSave(holder);
        return true;
    }

    @Override
    public boolean clearPermissions() {
        if (enduring) {
            holder.clearNodes();
        } else {
            holder.clearTransientNodes();
        }
        superClass.objectSave(holder);
        return true;
    }

    @Override
    public boolean clearPermissions(Set<Context> set) {
        Map<String, String> context = set.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));
        List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                .filter(node -> node.shouldApplyWithContext(context))
                .collect(Collectors.toList());

        toRemove.forEach(n -> {
            try {
                if (enduring) {
                    holder.unsetPermission(n);
                } else {
                    holder.unsetTransientPermission(n);
                }
            } catch (ObjectLacksException ignored) {}
        });

        if (holder instanceof User) {
            service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
        }

        superClass.objectSave(holder);
        return !toRemove.isEmpty();
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        Map<Set<Context>, List<Subject>> parents = new HashMap<>();

        for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
            if (!n.isGroupNode()) {
                continue;
            }

            Set<Context> contexts = n.getExtraContexts().entrySet().stream()
                    .map(entry -> new Context(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet());

            if (n.isServerSpecific()) {
                contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
            }

            if (n.isWorldSpecific()) {
                contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
            }

            if (!parents.containsKey(contexts)) {
                parents.put(contexts, new ArrayList<>());
            }

            parents.get(contexts).add(service.getGroupSubjects().get(n.getGroupName()));
        }

        ImmutableMap.Builder<Set<Context>, List<Subject>> map = ImmutableMap.builder();
        for (Map.Entry<Set<Context>, List<Subject>> e : parents.entrySet()) {
            map.put(ImmutableSet.copyOf(e.getKey()), ImmutableList.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts) {
        return getAllParents().getOrDefault(contexts, ImmutableList.of());
    }

    @Override
    public boolean addParent(Set<Context> set, Subject subject) {
        if (subject instanceof LuckPermsSubject) {
            LuckPermsSubject permsSubject = ((LuckPermsSubject) subject);
            Map<String, String> contexts = set.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));

            try {
                if (enduring) {
                    holder.setPermission(new me.lucko.luckperms.core.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    holder.setTransientPermission(new me.lucko.luckperms.core.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                }
            } catch (ObjectAlreadyHasException ignored) {}
            superClass.objectSave(holder);
        } else {
            return false;
        }
        return false;
    }

    @Override
    public boolean removeParent(Set<Context> set, Subject subject) {
        if (subject instanceof LuckPermsSubject) {
            LuckPermsSubject permsSubject = ((LuckPermsSubject) subject);
            Map<String, String> contexts = set.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));

            try {
                if (enduring) {
                    holder.unsetPermission(new me.lucko.luckperms.core.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    holder.unsetTransientPermission(new me.lucko.luckperms.core.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                }
            } catch (ObjectLacksException ignored) {}
            superClass.objectSave(holder);
        } else {
            return false;
        }
        return false;
    }

    @Override
    public boolean clearParents() {
        List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                .filter(Node::isGroupNode)
                .collect(Collectors.toList());

        toRemove.forEach(n -> {
            try {
                if (enduring) {
                    holder.unsetPermission(n);
                } else {
                    holder.unsetTransientPermission(n);
                }
            } catch (ObjectLacksException ignored) {}
        });

        if (holder instanceof User) {
            service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
        }

        superClass.objectSave(holder);
        return !toRemove.isEmpty();
    }

    @Override
    public boolean clearParents(Set<Context> set) {
        Map<String, String> context = set.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));
        List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                .filter(Node::isGroupNode)
                .filter(node -> node.shouldApplyWithContext(context))
                .collect(Collectors.toList());

        toRemove.forEach(n -> {
            try {
                if (enduring) {
                    holder.unsetPermission(n);
                } else {
                    holder.unsetTransientPermission(n);
                }
            } catch (ObjectLacksException ignored) {}
        });

        if (holder instanceof User) {
            service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
        }

        superClass.objectSave(holder);
        return !toRemove.isEmpty();
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        Map<Set<Context>, Map<String, String>> options = new HashMap<>();

        for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
            if (!n.isMeta() && !n.isPrefix() && !n.isSuffix()) {
                continue;
            }

            Set<Context> contexts = n.getExtraContexts().entrySet().stream()
                    .map(entry -> new Context(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet());

            if (n.isServerSpecific()) {
                contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
            }

            if (n.isWorldSpecific()) {
                contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
            }

            if (!options.containsKey(contexts)) {
                options.put(contexts, new HashMap<>());
            }

            options.get(contexts).put(unescapeCharacters(n.getMeta().getKey()), unescapeCharacters(n.getMeta().getValue()));
        }

        ImmutableMap.Builder<Set<Context>, Map<String, String>> map = ImmutableMap.builder();
        for (Map.Entry<Set<Context>, Map<String, String>> e : options.entrySet()) {
            map.put(ImmutableSet.copyOf(e.getKey()), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public Map<String, String> getOptions(Set<Context> set) {
        return getAllOptions().getOrDefault(set, Collections.emptyMap());
    }

    @Override
    public boolean setOption(Set<Context> set, String key, String value) {
        Map<String, String> context = new HashMap<>();
        for (Context c : set) {
            context.put(c.getKey(), c.getValue());
        }

        key = escapeCharacters(key);
        value = escapeCharacters(value);

        try {
            if (enduring) {
                holder.setPermission(new me.lucko.luckperms.core.Node.Builder("meta." + key + "." + value)
                        .withExtraContext(context)
                        .build()
                );
            } else {
                holder.setTransientPermission(new me.lucko.luckperms.core.Node.Builder("meta." + key + "." + value)
                        .withExtraContext(context)
                        .build()
                );
            }
        } catch (ObjectAlreadyHasException ignored) {}
        superClass.objectSave(holder);
        return true;
    }

    @Override
    public boolean clearOptions(Set<Context> set) {
        Map<String, String> context = set.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));
        List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                .filter(node -> node.shouldApplyWithContext(context))
                .collect(Collectors.toList());

        toRemove.forEach(n -> {
            try {
                if (enduring) {
                    holder.unsetPermission(n);
                } else {
                    holder.unsetTransientPermission(n);
                }
            } catch (ObjectLacksException ignored) {}
        });

        superClass.objectSave(holder);
        return !toRemove.isEmpty();
    }

    @Override
    public boolean clearOptions() {
        List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                .collect(Collectors.toList());

        toRemove.forEach(n -> {
            try {
                if (enduring) {
                    holder.unsetPermission(n);
                } else {
                    holder.unsetTransientPermission(n);
                }
            } catch (ObjectLacksException ignored) {}
        });

        superClass.objectSave(holder);
        return !toRemove.isEmpty();
    }
}
