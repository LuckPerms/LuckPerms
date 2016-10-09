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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.core.PermissionHolder;
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

import static me.lucko.luckperms.utils.ArgumentChecker.unescapeCharacters;

@EqualsAndHashCode(of = {"holder"})
public class LuckPermsSubject implements Subject {
    public static LuckPermsSubject wrapHolder(PermissionHolder holder, LuckPermsService service) {
        return new LuckPermsSubject(holder, service);
    }

    @Getter
    private PermissionHolder holder;
    private LuckPermsSubjectData enduringData;
    private LuckPermsSubjectData transientData;
    protected LuckPermsService service;

    LuckPermsSubject(PermissionHolder holder, LuckPermsService service) {
        this.holder = holder;
        this.enduringData = new LuckPermsSubjectData(true, this, service, holder);
        this.transientData = new LuckPermsSubjectData(true, this, service, holder);
        this.service = service;
    }

    public void deprovision() {
        holder = null;
        enduringData = null;
        transientData = null;
        service = null;
    }

    void objectSave(PermissionHolder t) {
        service.getPlugin().doAsync(() -> {
            if (t instanceof User) {
                ((User) t).refreshPermissions();
                service.getPlugin().getDatastore().saveUser(((User) t));
            }
            if (t instanceof Group) {
                service.getPlugin().getDatastore().saveGroup(((Group) t));
                service.getPlugin().runUpdateTask();
            }
        });
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

    public boolean isPermissionSet(@NonNull Set<Context> contexts, @NonNull String node) {
        return getPermissionValue(contexts, node) != Tristate.UNDEFINED;
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

        switch (holder.inheritsPermission(new me.lucko.luckperms.core.Node.Builder(node).withExtraContext(context).build())) {
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
            String prefix = getChatMeta(set, true, holder);
            if (!prefix.equals("")) {
                return Optional.of(prefix);
            }
        }

        if (s.equalsIgnoreCase("suffix")) {
            String suffix = getChatMeta(set, false, holder);
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

    private String getChatMeta(Set<Context> contexts, boolean prefix, PermissionHolder holder) {
        if (holder == null) return "";

        Map<String, String> context = contexts.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));
        String server = context.get("server");
        String world = context.get("world");
        context.remove("server");
        context.remove("world");

        int priority = Integer.MIN_VALUE;
        String meta = null;

        for (Node n : holder.getAllNodes(null, Contexts.allowAll())) {
            if (!n.getValue()) {
                continue;
            }

            if (prefix ? !n.isPrefix() : !n.isSuffix()) {
                continue;
            }

            if (!n.shouldApplyOnServer(server, service.getPlugin().getConfiguration().isVaultIncludingGlobal(), false)) {
                continue;
            }

            if (!n.shouldApplyOnWorld(world, true, false)) {
                continue;
            }

            if (!n.shouldApplyWithContext(context, false)) {
                continue;
            }

            Map.Entry<Integer, String> value = prefix ? n.getPrefix() : n.getSuffix();
            if (value.getKey() > priority) {
                meta = value.getValue();
                priority = value.getKey();
            }
        }

        return meta == null ? "" : unescapeCharacters(meta);
    }
}
