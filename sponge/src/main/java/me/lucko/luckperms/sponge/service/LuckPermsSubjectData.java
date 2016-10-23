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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.stream.Collectors;

import static me.lucko.luckperms.api.MetaUtils.escapeCharacters;

@AllArgsConstructor
public class LuckPermsSubjectData implements SubjectData {
    private final boolean enduring;
    private final LuckPermsService service;

    @Getter
    private final PermissionHolder holder;

    private void objectSave(PermissionHolder t) {
        if (t instanceof User) {
            service.getPlugin().getDatastore().saveUser(((User) t), b -> ((User) t).getRefreshBuffer().request());
        }
        if (t instanceof Group) {
            service.getPlugin().getDatastore().saveGroup(((Group) t), b -> service.getPlugin().getUpdateTaskBuffer().request());
        }
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
        Map<Set<Context>, Map<String, Boolean>> perms = new HashMap<>();

        for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
            Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

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
    public Map<String, Boolean> getPermissions(Set<Context> contexts) {
        ImmutableMap.Builder<String, Boolean> permissions = ImmutableMap.builder();

        (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                .filter(n -> n.shouldApplyWithContext(LuckPermsService.convertContexts(contexts), true))
                .forEach(n -> permissions.put(n.getKey(), n.getValue()));
        
        return permissions.build();
    }

    @Override
    public boolean setPermission(Set<Context> set, String s, Tristate tristate) {
        if (tristate == Tristate.UNDEFINED) {
            // Unset
            Node.Builder builder = new me.lucko.luckperms.common.core.Node.Builder(s);

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

            objectSave(holder);
            return true;
        }

        Node.Builder builder = new me.lucko.luckperms.common.core.Node.Builder(s)
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

        objectSave(holder);
        return true;
    }

    @Override
    public boolean clearPermissions() {
        if (enduring) {
            holder.clearNodes();
        } else {
            holder.clearTransientNodes();
        }
        objectSave(holder);
        return true;
    }

    @Override
    public boolean clearPermissions(Set<Context> contexts) {
        List<Node> toRemove = (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                .filter(node -> node.shouldApplyWithContext(LuckPermsService.convertContexts(contexts)))
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

        objectSave(holder);
        return !toRemove.isEmpty();
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        Map<Set<Context>, List<Subject>> parents = new HashMap<>();

        for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
            if (!n.isGroupNode()) {
                continue;
            }

            Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

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
        ImmutableList.Builder<Subject> parents = ImmutableList.builder();

        (enduring ? holder.getNodes() : holder.getTransientNodes()).stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyWithContext(LuckPermsService.convertContexts(contexts), true))
                .forEach(n -> parents.add(service.getGroupSubjects().get(n.getGroupName())));

        return parents.build();
    }

    @Override
    public boolean addParent(Set<Context> set, Subject subject) {
        if (subject instanceof LuckPermsGroupSubject) {
            LuckPermsGroupSubject permsSubject = ((LuckPermsGroupSubject) subject);
            ContextSet contexts = LuckPermsService.convertContexts(set);

            try {
                if (enduring) {
                    holder.setPermission(new me.lucko.luckperms.common.core.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    holder.setTransientPermission(new me.lucko.luckperms.common.core.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                }
            } catch (ObjectAlreadyHasException ignored) {}

            objectSave(holder);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeParent(Set<Context> set, Subject subject) {
        if (subject instanceof LuckPermsGroupSubject) {
            LuckPermsGroupSubject permsSubject = ((LuckPermsGroupSubject) subject);
            ContextSet contexts = LuckPermsService.convertContexts(set);

            try {
                if (enduring) {
                    holder.unsetPermission(new me.lucko.luckperms.common.core.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } else {
                    holder.unsetTransientPermission(new me.lucko.luckperms.common.core.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                }
            } catch (ObjectLacksException ignored) {}

            objectSave(holder);
            return true;
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

        objectSave(holder);
        return !toRemove.isEmpty();
    }

    @Override
    public boolean clearParents(Set<Context> set) {
        ContextSet context = LuckPermsService.convertContexts(set);

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

        objectSave(holder);
        return !toRemove.isEmpty();
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        Map<Set<Context>, Map<String, String>> options = new HashMap<>();

        int prefixPriority = Integer.MIN_VALUE;
        int suffixPriority = Integer.MIN_VALUE;

        for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
            if (!n.getValue()) {
                continue;
            }

            if (!n.isMeta() || !n.isPrefix() || n.isSuffix()) {
                continue;
            }

            Set<Context> contexts = LuckPermsService.convertContexts(n.getContexts());

            if (n.isServerSpecific()) {
                contexts.add(new Context(LuckPermsService.SERVER_CONTEXT, n.getServer().get()));
            }

            if (n.isWorldSpecific()) {
                contexts.add(new Context(Context.WORLD_KEY, n.getWorld().get()));
            }

            if (!options.containsKey(contexts)) {
                options.put(contexts, new HashMap<>());
            }

            if (n.isPrefix()) {
                Map.Entry<Integer, String> value = n.getPrefix();
                if (value.getKey() > prefixPriority) {
                    options.get(contexts).put("prefix", value.getValue());
                    prefixPriority = value.getKey();
                }
                continue;
            }

            if (n.isSuffix()) {
                Map.Entry<Integer, String> value = n.getSuffix();
                if (value.getKey() > suffixPriority) {
                    options.get(contexts).put("suffix", value.getValue());
                    suffixPriority = value.getKey();
                }
                continue;
            }

            if (n.isMeta()) {
                Map.Entry<String, String> meta = n.getMeta();
                options.get(contexts).put(meta.getKey(), meta.getValue());
            }
        }

        ImmutableMap.Builder<Set<Context>, Map<String, String>> map = ImmutableMap.builder();
        for (Map.Entry<Set<Context>, Map<String, String>> e : options.entrySet()) {
            map.put(ImmutableSet.copyOf(e.getKey()), ImmutableMap.copyOf(e.getValue()));
        }
        return map.build();
    }

    @Override
    public Map<String, String> getOptions(Set<Context> set) {
        ImmutableMap.Builder<String, String> options = ImmutableMap.builder();
        ContextSet contexts = LuckPermsService.convertContexts(set);

        int prefixPriority = Integer.MIN_VALUE;
        int suffixPriority = Integer.MIN_VALUE;

        for (Node n : enduring ? holder.getNodes() : holder.getTransientNodes()) {
            if (!n.getValue()) {
                continue;
            }

            if (!n.isMeta() || !n.isPrefix() || n.isSuffix()) {
                continue;
            }

            if (!n.shouldApplyWithContext(contexts, true)) {
                continue;
            }

            if (n.isPrefix()) {
                Map.Entry<Integer, String> value = n.getPrefix();
                if (value.getKey() > prefixPriority) {
                    options.put("prefix", value.getValue());
                    prefixPriority = value.getKey();
                }
                continue;
            }

            if (n.isSuffix()) {
                Map.Entry<Integer, String> value = n.getSuffix();
                if (value.getKey() > suffixPriority) {
                    options.put("suffix", value.getValue());
                    suffixPriority = value.getKey();
                }
                continue;
            }

            if (n.isMeta()) {
                Map.Entry<String, String> meta = n.getMeta();
                options.put(meta.getKey(), meta.getValue());
            }
        }

        return options.build();
    }

    @Override
    public boolean setOption(Set<Context> set, String key, String value) {
        ContextSet context = LuckPermsService.convertContexts(set);

        key = escapeCharacters(key);
        value = escapeCharacters(value);

        try {
            if (enduring) {
                holder.setPermission(new me.lucko.luckperms.common.core.Node.Builder("meta." + key + "." + value)
                        .withExtraContext(context)
                        .build()
                );
            } else {
                holder.setTransientPermission(new me.lucko.luckperms.common.core.Node.Builder("meta." + key + "." + value)
                        .withExtraContext(context)
                        .build()
                );
            }
        } catch (ObjectAlreadyHasException ignored) {}
        objectSave(holder);
        return true;
    }

    @Override
    public boolean clearOptions(Set<Context> set) {
        ContextSet context = LuckPermsService.convertContexts(set);

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

        objectSave(holder);
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

        objectSave(holder);
        return !toRemove.isEmpty();
    }
}
