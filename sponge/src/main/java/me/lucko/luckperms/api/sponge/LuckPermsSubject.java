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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.stream.Collectors;

import static me.lucko.luckperms.utils.ArgumentChecker.escapeCharacters;
import static me.lucko.luckperms.utils.ArgumentChecker.unescapeCharacters;

@EqualsAndHashCode(of = {"holder"})
public class LuckPermsSubject implements Subject {
    public static Subject wrapHolder(PermissionHolder holder, LuckPermsService service) {
        return new LuckPermsSubject(holder, service);
    }

    @Getter
    private final PermissionHolder holder;
    private final EnduringData enduringData;
    private final TransientData transientData;
    protected final LuckPermsService service;

    LuckPermsSubject(PermissionHolder holder, LuckPermsService service) {
        this.holder = holder;
        this.enduringData = new EnduringData(this, service, holder);
        this.transientData = new TransientData(service, holder);
        this.service = service;
    }

    private void objectSave(PermissionHolder t) {
        if (t instanceof User) {
            ((User) t).refreshPermissions();
            service.getPlugin().getDatastore().saveUser(((User) t), Callback.empty());
        }
        if (t instanceof Group) {
            service.getPlugin().getDatastore().saveGroup(((Group) t), c -> service.getPlugin().runUpdateTask());
        }
    }

    @Override
    public String getIdentifier() {
        return holder.getObjectName();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
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
        return enduringData;
    }

    @Override
    public SubjectData getTransientSubjectData() {
        return transientData;
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
        Map<String, String> context = new HashMap<>();
        for (Context c : contexts) {
            context.put(c.getKey(), c.getValue());
        }

        switch (holder.inheritsPermission(new me.lucko.luckperms.utils.Node.Builder(node).withExtraContext(context).build())) {
            case UNDEFINED:
                return Tristate.UNDEFINED;
            case TRUE:
                return Tristate.TRUE;
            case FALSE:
                return Tristate.FALSE;
            default:
                return null;
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
        List<Subject> parents = new ArrayList<>();
        parents.addAll(enduringData.getParents(contexts));
        parents.addAll(transientData.getParents(contexts));
        return ImmutableList.copyOf(parents);
    }

    @Override
    public Optional<String> getOption(Set<Context> set, String s) {
        if (s.equalsIgnoreCase("prefix")) {
            String prefix = getChatMeta(true, holder);
            if (!prefix.equals("")) {
                return Optional.of(prefix);
            }
        }

        if (s.equalsIgnoreCase("suffix")) {
            String suffix = getChatMeta(false, holder);
            if (!suffix.equals("")) {
                return Optional.of(suffix);
            }
        }

        Map<String, String> transientOptions = enduringData.getOptions(set);
        if (transientOptions.containsKey(s)) {
            return Optional.of(transientOptions.get(s));
        }

        Map<String, String> enduringOptions = enduringData.getOptions(set);
        if (enduringOptions.containsKey(s)) {
            return Optional.of(enduringOptions.get(s));
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getOption(String key) {
        return getOption(getActiveContexts(), key);
    }

    @Override
    public Set<Context> getActiveContexts() {
        return SubjectData.GLOBAL_CONTEXT;
    }

    private String getChatMeta(boolean prefix, PermissionHolder holder) {
        if (holder == null) return "";

        int priority = Integer.MIN_VALUE;
        String meta = null;

        for (Node n : holder.getAllNodes(null)) {
            if (!n.getValue()) {
                continue;
            }

            if (prefix ? !n.isPrefix() : !n.isSuffix()) {
                continue;
            }

            if (!n.shouldApplyOnServer(service.getPlugin().getConfiguration().getVaultServer(), service.getPlugin().getConfiguration().getVaultIncludeGlobal(), false)) {
                continue;
            }

            /* TODO per world
            if (!n.shouldApplyOnWorld(world, service.getPlugin().getConfiguration().getVaultIncludeGlobal(), false)) {
                continue;
            }
            */

            Map.Entry<Integer, String> value = prefix ? n.getPrefix() : n.getSuffix();
            if (value.getKey() > priority) {
                meta = value.getValue();
                priority = value.getKey();
            }
        }

        return meta == null ? "" : unescapeCharacters(meta);
    }

    @AllArgsConstructor
    public static class EnduringData implements SubjectData {
        private final LuckPermsSubject superClass;
        private final LuckPermsService service;

        @Getter
        private final PermissionHolder holder;

        @Override
        public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
            Map<Set<Context>, Map<String, Boolean>> perms = new HashMap<>();

            for (Node n : holder.getNodes()) {
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

            return ImmutableMap.copyOf(perms);
        }

        @Override
        public Map<String, Boolean> getPermissions(Set<Context> set) {
            return ImmutableMap.copyOf(getAllPermissions().getOrDefault(set, Collections.emptyMap()));
        }

        @Override
        public boolean setPermission(Set<Context> set, String s, Tristate tristate) {
            if (tristate == Tristate.UNDEFINED) {
                // Unset
                Node.Builder builder = new me.lucko.luckperms.utils.Node.Builder(s);

                for (Context ct : set) {
                    builder.withExtraContext(ct.getKey(), ct.getValue());
                }

                try {
                    holder.unsetPermission(builder.build());
                } catch (ObjectLacksException ignored) {}
                superClass.objectSave(holder);
                return true;
            }

            Node.Builder builder = new me.lucko.luckperms.utils.Node.Builder(s)
                    .setValue(tristate.asBoolean());

            for (Context ct : set) {
                builder.withExtraContext(ct.getKey(), ct.getValue());
            }

            try {
                holder.setPermission(builder.build());
            } catch (ObjectAlreadyHasException ignored) {}
            superClass.objectSave(holder);
            return true;
        }

        @Override
        public boolean clearPermissions() {
            holder.getNodes().clear();
            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }
            superClass.objectSave(holder);
            return true;
        }

        @Override
        public boolean clearPermissions(Set<Context> set) {
            Map<String, String> context = new HashMap<>();
            for (Context c : set) {
                context.put(c.getKey(), c.getValue());
            }

            boolean work = false;
            Iterator<Node> iterator = holder.getNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();
                if (entry.shouldApplyWithContext(context)) {
                    iterator.remove();
                    work = true;
                }
            }

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            superClass.objectSave(holder);
            return work;
        }

        @Override
        public Map<Set<Context>, List<Subject>> getAllParents() {
            Map<Set<Context>, List<Subject>> parents = new HashMap<>();

            for (Node n : holder.getAllNodes(null)) {
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

            return ImmutableMap.copyOf(parents);
        }

        @Override
        public List<Subject> getParents(Set<Context> contexts) {
            return ImmutableList.copyOf(getAllParents().getOrDefault(contexts, Collections.emptyList()));
        }

        @Override
        public boolean addParent(Set<Context> set, Subject subject) {
            if (subject instanceof LuckPermsSubject) {
                LuckPermsSubject permsSubject = ((LuckPermsSubject) subject);

                Map<String, String> contexts = set.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));

                try {
                    holder.setPermission(new me.lucko.luckperms.utils.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
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
                    holder.unsetPermission(new me.lucko.luckperms.utils.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } catch (ObjectLacksException ignored) {}
                superClass.objectSave(holder);
            } else {
                return false;
            }
            return false;
        }

        @Override
        public boolean clearParents() {
            boolean work = false;
            Iterator<Node> iterator = holder.getNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();

                if (entry.isGroupNode()) {
                    iterator.remove();
                    work = true;
                }
            }

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            superClass.objectSave(holder);
            return work;
        }

        @Override
        public boolean clearParents(Set<Context> set) {
            Map<String, String> context = new HashMap<>();
            for (Context c : set) {
                context.put(c.getKey(), c.getValue());
            }

            boolean work = false;
            Iterator<Node> iterator = holder.getNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();

                if (!entry.isGroupNode()) {
                    continue;
                }

                if (entry.shouldApplyWithContext(context)) {
                    iterator.remove();
                    work = true;
                }
            }

            if (holder instanceof User) {
                service.getPlugin().getUserManager().giveDefaultIfNeeded(((User) holder), false);
            }

            superClass.objectSave(holder);
            return work;
        }

        @Override
        public Map<Set<Context>, Map<String, String>> getAllOptions() {
            Map<Set<Context>, Map<String, String>> options = new HashMap<>();

            for (Node n : holder.getAllNodes(null)) {
                if (!n.isMeta()) {
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

            return ImmutableMap.copyOf(options);
        }

        @Override
        public Map<String, String> getOptions(Set<Context> set) {
            return ImmutableMap.copyOf(getAllOptions().getOrDefault(set, Collections.emptyMap()));
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
                holder.setPermission(new me.lucko.luckperms.utils.Node.Builder("meta." + key + "." + value)
                        .withExtraContext(context)
                        .build()
                );
            } catch (ObjectAlreadyHasException ignored) {}
            superClass.objectSave(holder);
            return true;
        }

        @Override
        public boolean clearOptions(Set<Context> set) {
            Map<String, String> context = new HashMap<>();
            for (Context c : set) {
                context.put(c.getKey(), c.getValue());
            }

            boolean work = false;
            Iterator<Node> iterator = holder.getNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();

                if (!entry.isMeta()) {
                    continue;
                }

                if (entry.shouldApplyWithContext(context)) {
                    iterator.remove();
                    work = true;
                }
            }

            superClass.objectSave(holder);
            return work;
        }

        @Override
        public boolean clearOptions() {
            boolean work = false;
            Iterator<Node> iterator = holder.getNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();

                if (entry.isMeta()) {
                    iterator.remove();
                    work = true;
                }
            }

            superClass.objectSave(holder);
            return work;
        }
    }

    @AllArgsConstructor
    public static class TransientData implements SubjectData {
        private final LuckPermsService service;

        @Getter
        private final PermissionHolder holder;

        @Override
        public Map<Set<Context>, Map<String, Boolean>> getAllPermissions() {
            Map<Set<Context>, Map<String, Boolean>> perms = new HashMap<>();

            for (Node n : holder.getTransientNodes()) {
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

            return ImmutableMap.copyOf(perms);
        }

        @Override
        public Map<String, Boolean> getPermissions(Set<Context> set) {
            return ImmutableMap.copyOf(getAllPermissions().getOrDefault(set, Collections.emptyMap()));
        }

        @Override
        public boolean setPermission(Set<Context> set, String s, Tristate tristate) {
            if (tristate == Tristate.UNDEFINED) {
                // Unset

                Node.Builder builder = new me.lucko.luckperms.utils.Node.Builder(s);

                for (Context ct : set) {
                    builder.withExtraContext(ct.getKey(), ct.getValue());
                }

                try {
                    holder.unsetTransientPermission(builder.build());
                } catch (ObjectLacksException ignored) {}
                return true;
            }

            Node.Builder builder = new me.lucko.luckperms.utils.Node.Builder(s)
                    .setValue(tristate.asBoolean());

            for (Context ct : set) {
                builder.withExtraContext(ct.getKey(), ct.getValue());
            }

            try {
                holder.setTransientPermission(builder.build());
            } catch (ObjectAlreadyHasException ignored) {}
            return true;
        }

        @Override
        public boolean clearPermissions() {
            holder.getTransientNodes().clear();
            return true;
        }

        @Override
        public boolean clearPermissions(Set<Context> set) {
            Map<String, String> context = new HashMap<>();
            for (Context c : set) {
                context.put(c.getKey(), c.getValue());
            }

            boolean work = false;
            Iterator<Node> iterator = holder.getTransientNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();
                if (entry.shouldApplyWithContext(context)) {
                    iterator.remove();
                    work = true;
                }
            }

            return work;
        }

        @Override
        public Map<Set<Context>, List<Subject>> getAllParents() {
            Map<Set<Context>, List<Subject>> parents = new HashMap<>();

            for (Node n : holder.getTransientNodes()) {
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

            return ImmutableMap.copyOf(parents);
        }

        @Override
        public List<Subject> getParents(Set<Context> contexts) {
            return ImmutableList.copyOf(getAllParents().getOrDefault(contexts, Collections.emptyList()));
        }

        @Override
        public boolean addParent(Set<Context> set, Subject subject) {
            if (subject instanceof LuckPermsSubject) {
                LuckPermsSubject permsSubject = ((LuckPermsSubject) subject);

                Map<String, String> contexts = set.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));

                try {
                    holder.setTransientPermission(new me.lucko.luckperms.utils.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } catch (ObjectAlreadyHasException ignored) {}
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
                    holder.unsetTransientPermission(new me.lucko.luckperms.utils.Node.Builder("group." + permsSubject.getIdentifier())
                            .withExtraContext(contexts)
                            .build());
                } catch (ObjectLacksException ignored) {}
            } else {
                return false;
            }
            return false;
        }

        @Override
        public boolean clearParents() {
            boolean work = false;
            Iterator<Node> iterator = holder.getTransientNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();

                if (entry.isGroupNode()) {
                    iterator.remove();
                    work = true;
                }
            }

            return work;
        }

        @Override
        public boolean clearParents(Set<Context> set) {
            Map<String, String> context = new HashMap<>();
            for (Context c : set) {
                context.put(c.getKey(), c.getValue());
            }

            boolean work = false;
            Iterator<Node> iterator = holder.getTransientNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();

                if (!entry.isGroupNode()) {
                    continue;
                }

                if (entry.shouldApplyWithContext(context)) {
                    iterator.remove();
                    work = true;
                }
            }

            return work;
        }

        @Override
        public Map<Set<Context>, Map<String, String>> getAllOptions() {
            Map<Set<Context>, Map<String, String>> options = new HashMap<>();

            for (Node n : holder.getTransientNodes()) {
                if (!n.isMeta()) {
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

            return ImmutableMap.copyOf(options);
        }

        @Override
        public Map<String, String> getOptions(Set<Context> set) {
            return ImmutableMap.copyOf(getAllOptions().getOrDefault(set, Collections.emptyMap()));
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
                holder.setTransientPermission(new me.lucko.luckperms.utils.Node.Builder("meta." + key + "." + value)
                        .withExtraContext(context)
                        .build()
                );
            } catch (ObjectAlreadyHasException ignored) {}
            return true;
        }

        @Override
        public boolean clearOptions(Set<Context> set) {
            Map<String, String> context = new HashMap<>();
            for (Context c : set) {
                context.put(c.getKey(), c.getValue());
            }

            boolean work = false;
            Iterator<Node> iterator = holder.getTransientNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();

                if (!entry.isMeta()) {
                    continue;
                }

                if (entry.shouldApplyWithContext(context)) {
                    iterator.remove();
                    work = true;
                }
            }

            return work;
        }

        @Override
        public boolean clearOptions() {
            boolean work = false;
            Iterator<Node> iterator = holder.getTransientNodes().iterator();

            while (iterator.hasNext()) {
                Node entry = iterator.next();

                if (entry.isMeta()) {
                    iterator.remove();
                    work = true;
                }
            }

            return work;
        }
    }
}
