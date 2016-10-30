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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.users.User;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@EqualsAndHashCode(of = "user", callSuper = false)
public class LuckPermsUserSubject extends LuckPermsSubject {
    public static LuckPermsUserSubject wrapUser(User user, LuckPermsService service) {
        return new LuckPermsUserSubject(user, service);
    }

    @Getter(value = AccessLevel.NONE)
    private LuckPermsService service;

    private User user;
    private LuckPermsSubjectData subjectData;
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
    public Tristate getPermissionValue(ContextSet contexts, String permission) {
        return !hasData() ?
                Tristate.UNDEFINED :
                LuckPermsService.convertTristate(user.getUserData().getPermissionData(service.calculateContexts(contexts)).getPermissionValue(permission));
    }

    @Override
    public boolean isChildOf(ContextSet contexts, Subject parent) {
        return parent instanceof LuckPermsGroupSubject && getPermissionValue(contexts, "group." + parent.getIdentifier()).asBoolean();
    }

    @Override
    public List<Subject> getParents(ContextSet contexts) {
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

        subjects.addAll(service.getUserSubjects().getDefaults().getParents(LuckPermsService.convertContexts(contexts)));
        subjects.addAll(service.getDefaults().getParents(LuckPermsService.convertContexts(contexts)));

        return subjects.build();
    }

    @Override
    public Optional<String> getOption(ContextSet contexts, String s) {
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

        Optional<String> v = service.getUserSubjects().getDefaults().getOption(LuckPermsService.convertContexts(contexts), s);
        if (v.isPresent()) {
            return v;
        }

        return service.getDefaults().getOption(LuckPermsService.convertContexts(contexts), s);
    }

    @Override
    public ContextSet getActiveContextSet() {
        return service.getPlugin().getContextManager().getApplicableContext(this);
    }
}
