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

package me.lucko.luckperms.sponge.model;

import lombok.Getter;

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.LuckPermsSubjectData;
import me.lucko.luckperms.sponge.service.base.LPSubject;
import me.lucko.luckperms.sponge.service.references.SubjectCollectionReference;
import me.lucko.luckperms.sponge.service.references.SubjectReference;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionService;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import co.aikar.timings.Timing;

public class SpongeUser extends User {

    @Getter
    private final UserSubject spongeData;

    public SpongeUser(UUID uuid, LPSpongePlugin plugin) {
        super(uuid, plugin);
        this.spongeData = new UserSubject(plugin, this);
    }

    public SpongeUser(UUID uuid, String name, LPSpongePlugin plugin) {
        super(uuid, name, plugin);
        this.spongeData = new UserSubject(plugin, this);
    }

    public static class UserSubject implements LPSubject {
        private final SpongeUser parent;
        private final LPSpongePlugin plugin;

        @Getter
        private final LuckPermsSubjectData subjectData;

        @Getter
        private final LuckPermsSubjectData transientSubjectData;

        private UserSubject(LPSpongePlugin plugin, SpongeUser parent) {
            this.parent = parent;
            this.plugin = plugin;
            this.subjectData = new LuckPermsSubjectData(true, plugin.getService(), parent, this);
            this.transientSubjectData = new LuckPermsSubjectData(false, plugin.getService(), parent, this);
        }

        private boolean hasData() {
            return parent.getUserData() != null;
        }

        @Override
        public String getIdentifier() {
            return plugin.getUuidCache().getExternalUUID(parent.getUuid()).toString();
        }

        @Override
        public Optional<CommandSource> getCommandSource() {
            final UUID uuid = plugin.getUuidCache().getExternalUUID(parent.getUuid());

            Optional<Player> p = Sponge.getServer().getPlayer(uuid);
            if (p.isPresent()) {
                return Optional.of(p.get());
            }

            return Optional.empty();
        }

        @Override
        public SubjectCollectionReference getParentCollection() {
            return plugin.getService().getUserSubjects().toReference();
        }

        @Override
        public LuckPermsService getService() {
            return plugin.getService();
        }

        @Override
        public Tristate getPermissionValue(ContextSet contexts, String permission) {
            try (Timing ignored = plugin.getTimings().time(LPTiming.USER_GET_PERMISSION_VALUE)) {
                if (!hasData()) {
                    return Tristate.UNDEFINED;
                }

                return parent.getUserData().getPermissionData(plugin.getService().calculateContexts(contexts)).getPermissionValue(permission);
            }
        }

        @Override
        public boolean isChildOf(ContextSet contexts, SubjectReference parent) {
            try (Timing ignored = plugin.getTimings().time(LPTiming.USER_IS_CHILD_OF)) {
                return parent.getCollection().equals(PermissionService.SUBJECTS_GROUP) && getPermissionValue(contexts, "group." + parent.getIdentifier()).asBoolean();
            }
        }

        @Override
        public Set<SubjectReference> getParents(ContextSet contexts) {
            try (Timing ignored = plugin.getTimings().time(LPTiming.USER_GET_PARENTS)) {
                ImmutableSet.Builder<SubjectReference> subjects = ImmutableSet.builder();

                if (hasData()) {
                    for (String perm : parent.getUserData().getPermissionData(plugin.getService().calculateContexts(contexts)).getImmutableBacking().keySet()) {
                        if (!perm.startsWith("group.")) {
                            continue;
                        }

                        String groupName = perm.substring("group.".length());
                        if (plugin.getGroupManager().isLoaded(groupName)) {
                            subjects.add(plugin.getService().getGroupSubjects().get(groupName).toReference());
                        }
                    }
                }

                subjects.addAll(plugin.getService().getUserSubjects().getDefaultSubject().resolve(getService()).getParents(contexts));
                subjects.addAll(plugin.getService().getDefaults().getParents(contexts));

                return subjects.build();
            }
        }

        @Override
        public Optional<String> getOption(ContextSet contexts, String s) {
            try (Timing ignored = plugin.getTimings().time(LPTiming.USER_GET_OPTION)) {
                if (hasData()) {
                    MetaData data = parent.getUserData().getMetaData(plugin.getService().calculateContexts(contexts));
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

                Optional<String> v = plugin.getService().getUserSubjects().getDefaultSubject().resolve(getService()).getOption(contexts, s);
                if (v.isPresent()) {
                    return v;
                }

                return plugin.getService().getDefaults().getOption(contexts, s);
            }
        }

        @Override
        public ContextSet getActiveContextSet() {
            try (Timing ignored = plugin.getTimings().time(LPTiming.USER_GET_ACTIVE_CONTEXTS)) {
                return plugin.getContextManager().getApplicableContext(this);
            }
        }
    }

}
