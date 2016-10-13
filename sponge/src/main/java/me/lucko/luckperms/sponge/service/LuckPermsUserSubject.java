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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.common.caching.MetaData;
import me.lucko.luckperms.common.users.User;
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

@EqualsAndHashCode(of = "user")
public class LuckPermsUserSubject implements Subject {
    public static LuckPermsUserSubject wrapUser(User user, LuckPermsService service) {
        return new LuckPermsUserSubject(user, service);
    }

    @Getter
    private User user;

    private LuckPermsService service;

    @Getter
    private LuckPermsSubjectData subjectData;

    @Getter
    private LuckPermsSubjectData transientSubjectData;

    private LuckPermsUserSubject(User user, LuckPermsService service) {
        this.user = user;
        this.service = service;
        this.subjectData = new LuckPermsSubjectData(true, service, user);
        this.transientSubjectData = new LuckPermsSubjectData(false, service, user);
    }

    public void deprovision() {
        /* For some reason, Sponge holds onto User instances in a cache, which in turn, prevents LuckPerms data from being GCed.
           As well as unloading, we also remove all references to the User instances. */
        user = null;
        service = null;
        subjectData = null;
        transientSubjectData = null;
    }

    private boolean hasData() {
        return user.getUserData() != null;
    }

    @Override
    public String getIdentifier() {
        return service.getPlugin().getUuidCache().getExternalUUID(user.getUuid()).toString();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        final UUID uuid = service.getPlugin().getUuidCache().getExternalUUID(user.getUuid());

        Optional<Player> p = Sponge.getServer().getPlayer(uuid);
        if (p.isPresent()) {
            return Optional.of(p.get());
        }

        return Optional.empty();
    }

    @Override
    public SubjectCollection getContainingCollection() {
        return service.getUserSubjects();
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        return getPermissionValue(contexts, permission).asBoolean();
    }

    @Override
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String permission) {
        if (hasData()) {
            return LuckPermsService.convertTristate(user.getUserData().getPermissionData(service.calculateContexts(contexts)).getPermissionValue(permission));
        }

        return Tristate.UNDEFINED;
    }

    @Override
    public boolean isChildOf(@NonNull Set<Context> contexts, @NonNull Subject parent) {
        return parent instanceof LuckPermsGroupSubject && getPermissionValue(contexts, "group." + parent.getIdentifier()).asBoolean();
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts) {
        ImmutableList.Builder<Subject> subjects = ImmutableList.builder();

        if (hasData()) {
            for (String perm : user.getUserData().getPermissionData(service.calculateContexts(contexts)).getImmutableBacking().keySet()) {
                if (!perm.startsWith("group.")) {
                    continue;
                }

                String groupName = perm.substring("group.".length());
                if (service.getPlugin().getGroupManager().isLoaded(groupName)) {
                    subjects.add(service.getGroupSubjects().get(groupName));
                }
            }
        }

        subjects.addAll(service.getUserSubjects().getDefaults().getParents(contexts));
        subjects.addAll(service.getDefaults().getParents(contexts));

        return subjects.build();
    }

    @Override
    public Optional<String> getOption(Set<Context> contexts, String s) {
        if (hasData()) {
            MetaData data = user.getUserData().getMetaData(service.calculateContexts(contexts));
            if (s.equalsIgnoreCase("prefix")) {
                if (data.getPrefix() != null) {
                    return Optional.of(data.getPrefix());
                }
            }

            if (s.equalsIgnoreCase("suffix")) {
                if (data.getSuffix() != null) {
                    return Optional.of(data.getSuffix());
                }
            }

            if (data.getMeta().containsKey(s)) {
                return Optional.of(data.getMeta().get(s));
            }
        }

        Optional<String> v = service.getUserSubjects().getDefaults().getOption(contexts, s);
        if (v.isPresent()) {
            return v;
        }

        return service.getDefaults().getOption(contexts, s);
    }

    @Override
    public Set<Context> getActiveContexts() {
        final UUID uuid = service.getPlugin().getUuidCache().getExternalUUID(user.getUuid());
        Optional<Player> player = Sponge.getServer().getPlayer(uuid);

        if (!player.isPresent()) {
            return SubjectData.GLOBAL_CONTEXT;
        }

        Map<String, String> context = new HashMap<>();
        service.getPlugin().getContextManager().giveApplicableContext(player.get(), context);
        return context.entrySet().stream().map(e -> new Context(e.getKey(), e.getValue())).collect(Collectors.toSet());
    }
}
