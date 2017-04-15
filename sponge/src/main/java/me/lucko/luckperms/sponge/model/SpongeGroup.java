/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.caching.MetaAccumulator;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.utils.ExtractedContexts;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.LuckPermsSubjectData;
import me.lucko.luckperms.sponge.service.proxy.LPSubject;
import me.lucko.luckperms.sponge.service.proxy.Util;
import me.lucko.luckperms.sponge.service.references.SubjectCollectionReference;
import me.lucko.luckperms.sponge.service.references.SubjectReference;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.NodeTree;
import org.spongepowered.api.service.permission.PermissionService;

import co.aikar.timings.Timing;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SpongeGroup extends Group {

    @Getter
    private final GroupSubject spongeData;

    public SpongeGroup(String name, LPSpongePlugin plugin) {
        super(name, plugin);
        this.spongeData = new GroupSubject(plugin, this);
    }

    public static class GroupSubject implements LPSubject {

        @Getter
        private final SpongeGroup parent;

        @Getter
        private final LPSpongePlugin plugin;

        @Getter
        private final LuckPermsSubjectData subjectData;

        @Getter
        private final LuckPermsSubjectData transientSubjectData;

        private final LoadingCache<ContextSet, NodeTree> permissionCache = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(contexts -> {
                    // TODO move this away from NodeTree
                    Map<String, Boolean> permissions = getParent().getAllNodes(ExtractedContexts.generate(getPlugin().getService().calculateContexts(contexts))).stream()
                            .map(LocalizedNode::getNode)
                            .collect(Collectors.toMap(Node::getPermission, Node::getValue));

                    return NodeTree.of(permissions);
                });

        private final LoadingCache<ContextSet, Set<SubjectReference>> parentCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(contexts -> {
                    Set<SubjectReference> subjects = getParent().getAllNodes(ExtractedContexts.generate(getPlugin().getService().calculateContexts(contexts))).stream()
                            .map(LocalizedNode::getNode)
                            .filter(Node::isGroupNode)
                            .map(Node::getGroupName)
                            .map(s -> getPlugin().getService().getGroupSubjects().get(s))
                            .map(LPSubject::toReference)
                            .collect(Collectors.toSet());

                    subjects.addAll(getPlugin().getService().getGroupSubjects().getDefaultSubject().resolve(getService()).getParents(contexts));
                    subjects.addAll(getPlugin().getService().getDefaults().getParents(contexts));

                    return ImmutableSet.copyOf(subjects);
                });

        private GroupSubject(LPSpongePlugin plugin, SpongeGroup parent) {
            this.parent = parent;
            this.plugin = plugin;
            this.subjectData = new LuckPermsSubjectData(true, plugin.getService(), parent, this);
            this.transientSubjectData = new LuckPermsSubjectData(false, plugin.getService(), parent, this);

            parent.getStateListeners().add(this::invalidateCaches);
        }

        public void invalidateCaches() {
            permissionCache.invalidateAll();
            parentCache.invalidateAll();
        }

        @Override
        public void performCleanup() {
            permissionCache.cleanUp();
            parentCache.cleanUp();
        }

        @Override
        public String getIdentifier() {
            return parent.getObjectName();
        }

        @Override
        public Optional<String> getFriendlyIdentifier() {
            return Optional.of(parent.getFriendlyName());
        }

        @Override
        public Optional<CommandSource> getCommandSource() {
            return Optional.empty();
        }

        @Override
        public SubjectCollectionReference getParentCollection() {
            return plugin.getService().getGroupSubjects().toReference();
        }

        @Override
        public LuckPermsService getService() {
            return plugin.getService();
        }

        @Override
        public Tristate getPermissionValue(ContextSet contexts, String permission) {
            try (Timing ignored = plugin.getTimings().time(LPTiming.GROUP_GET_PERMISSION_VALUE)) {
                NodeTree nt = permissionCache.get(contexts);
                Tristate t = Util.convertTristate(nt.get(permission));
                if (t != Tristate.UNDEFINED) {
                    return t;
                }

                t = plugin.getService().getGroupSubjects().getDefaultSubject().resolve(getService()).getPermissionValue(contexts, permission);
                if (t != Tristate.UNDEFINED) {
                    return t;
                }

                t = plugin.getService().getDefaults().getPermissionValue(contexts, permission);
                return t;
            }
        }

        @Override
        public boolean isChildOf(ContextSet contexts, SubjectReference parent) {
            try (Timing ignored = plugin.getTimings().time(LPTiming.GROUP_IS_CHILD_OF)) {
                return parent.getCollection().equals(PermissionService.SUBJECTS_GROUP) && getPermissionValue(contexts, "group." + parent.getIdentifier()).asBoolean();
            }
        }

        @Override
        public Set<SubjectReference> getParents(ContextSet contexts) {
            try (Timing ignored = plugin.getTimings().time(LPTiming.GROUP_GET_PARENTS)) {
                return parentCache.get(contexts);
            }
        }

        @Override
        public Optional<String> getOption(ContextSet contexts, String s) {
            try (Timing ignored = plugin.getService().getPlugin().getTimings().time(LPTiming.GROUP_GET_OPTION)) {
                Optional<String> option;
                if (s.equalsIgnoreCase("prefix")) {
                    option = getChatMeta(contexts, true);

                } else if (s.equalsIgnoreCase("suffix")) {
                    option = getChatMeta(contexts, false);

                } else {
                    option = getMeta(contexts, s);
                }

                if (option.isPresent()) {
                    return option;
                }

                option = plugin.getService().getGroupSubjects().getDefaultSubject().resolve(getService()).getOption(contexts, s);
                if (option.isPresent()) {
                    return option;
                }

                return plugin.getService().getDefaults().getOption(contexts, s);
            }
        }

        @Override
        public ContextSet getActiveContextSet() {
            try (Timing ignored = plugin.getTimings().time(LPTiming.GROUP_GET_ACTIVE_CONTEXTS)) {
                return plugin.getContextManager().getApplicableContext(this);
            }
        }

        private Optional<String> getChatMeta(ContextSet contexts, boolean prefix) {
            MetaAccumulator metaAccumulator = parent.accumulateMeta(null, null, ExtractedContexts.generate(plugin.getService().calculateContexts(contexts)));
            if (prefix) {
                return Optional.ofNullable(metaAccumulator.getPrefixStack().toFormattedString());
            } else {
                return Optional.ofNullable(metaAccumulator.getSuffixStack().toFormattedString());
            }
        }

        private Optional<String> getMeta(ContextSet contexts, String key) {
            MetaAccumulator metaAccumulator = parent.accumulateMeta(null, null, ExtractedContexts.generate(plugin.getService().calculateContexts(contexts)));
            Map<String, String> meta = metaAccumulator.getMeta();
            return Optional.ofNullable(meta.get(key));
        }
    }
}
