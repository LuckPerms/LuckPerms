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
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.stream.Collectors;

import static me.lucko.luckperms.utils.ArgumentChecker.unescapeCharacters;

@EqualsAndHashCode(of = "group")
public class LuckPermsGroupSubject implements Subject {
    public static LuckPermsGroupSubject wrapGroup(Group group, LuckPermsService service) {
        return new LuckPermsGroupSubject(group, service);
    }

    @Getter
    private PermissionHolder group;

    private LuckPermsService service;

    @Getter
    private LuckPermsSubjectData subjectData;

    @Getter
    private LuckPermsSubjectData transientSubjectData;

    private LuckPermsGroupSubject(PermissionHolder group, LuckPermsService service) {
        this.group = group;
        this.subjectData = new LuckPermsSubjectData(true, service, group);
        this.transientSubjectData = new LuckPermsSubjectData(false, service, group);
        this.service = service;
    }

    @Override
    public String getIdentifier() {
        return group.getObjectName();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }

    @Override
    public SubjectCollection getContainingCollection() {
        return service.getGroupSubjects();
    }

    @Override
    public boolean hasPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        return getPermissionValue(contexts, node).asBoolean();
    }

    @Override
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String node) {
        Map<String, String> context = new HashMap<>();
        for (Context c : contexts) {
            context.put(c.getKey(), c.getValue());
        }

        switch (group.inheritsPermission(new me.lucko.luckperms.core.Node.Builder(node).withExtraContext(context).build())) {
            case UNDEFINED:
                return Tristate.UNDEFINED;
            case TRUE:
                return Tristate.TRUE;
            case FALSE:
                return Tristate.FALSE;
            default:
                return null;
        }

        // TODO
    }

    @Override
    public boolean isChildOf(@NonNull Set<Context> contexts, @NonNull Subject parent) {
        return parent instanceof LuckPermsGroupSubject && getPermissionValue(contexts, "group." + parent.getIdentifier()).asBoolean();
    }

    @Override
    public List<Subject> getParents(@NonNull Set<Context> contexts) {
        List<Subject> parents = new ArrayList<>();
        parents.addAll(subjectData.getParents(contexts));
        parents.addAll(transientSubjectData.getParents(contexts));
        return ImmutableList.copyOf(parents);
    }

    @Override
    public Optional<String> getOption(Set<Context> set, String s) {
        if (s.equalsIgnoreCase("prefix")) {
            String prefix = getChatMeta(set, true, group);
            if (!prefix.equals("")) {
                return Optional.of(prefix);
            }
        }

        if (s.equalsIgnoreCase("suffix")) {
            String suffix = getChatMeta(set, false, group);
            if (!suffix.equals("")) {
                return Optional.of(suffix);
            }
        }

        Map<String, String> transientOptions = subjectData.getOptions(set);
        if (transientOptions.containsKey(s)) {
            return Optional.of(transientOptions.get(s));
        }

        Map<String, String> enduringOptions = subjectData.getOptions(set);
        if (enduringOptions.containsKey(s)) {
            return Optional.of(enduringOptions.get(s));
        }

        return Optional.empty();
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
