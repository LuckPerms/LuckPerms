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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.caching.MetaAccumulator;
import me.lucko.luckperms.common.contexts.ExtractedContexts;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.LuckPermsSubjectData;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.CompatibilityUtil;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.NodeTree;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SpongeGroup extends Group {

    private final GroupSubject spongeData;

    public SpongeGroup(String name, LPSpongePlugin plugin) {
        super(name, plugin);
        this.spongeData = new GroupSubject(plugin, this);
    }

    public GroupSubject sponge() {
        return this.spongeData;
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

        private final LoadingCache<ImmutableContextSet, NodeTree> permissionCache = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build(contexts -> {
                    // TODO move this away from NodeTree
                    Map<String, Boolean> permissions = getParent().getAllNodes(ExtractedContexts.generate(getPlugin().getService().calculateContexts(contexts))).stream()
                            .map(LocalizedNode::getNode)
                            .collect(Collectors.toMap(Node::getPermission, Node::getValuePrimitive));

                    return NodeTree.of(permissions);
                });

        private final LoadingCache<ImmutableContextSet, ImmutableList<SubjectReference>> parentCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(contexts -> {
                    Set<SubjectReference> subjects = getParent().getAllNodes(ExtractedContexts.generate(getPlugin().getService().calculateContexts(contexts))).stream()
                            .map(LocalizedNode::getNode)
                            .filter(Node::isGroupNode)
                            .map(Node::getGroupName)
                            .distinct()
                            .map(n -> Optional.ofNullable(getPlugin().getGroupManager().getIfLoaded(n)))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(SpongeGroup::sponge)
                            .map(LPSubject::toReference)
                            .collect(Collectors.toSet());

                    subjects.addAll(getPlugin().getService().getGroupSubjects().getDefaults().getParents(contexts));
                    subjects.addAll(getPlugin().getService().getDefaults().getParents(contexts));

                    return getService().sortSubjects(subjects);
                });

        private GroupSubject(LPSpongePlugin plugin, SpongeGroup parent) {
            this.parent = parent;
            this.plugin = plugin;
            this.subjectData = new LuckPermsSubjectData(true, plugin.getService(), parent, this);
            this.transientSubjectData = new LuckPermsSubjectData(false, plugin.getService(), parent, this);

            parent.getStateListeners().add(() -> invalidateCaches(CacheLevel.PERMISSION));
        }

        @Override
        public void invalidateCaches(CacheLevel type) {
            if (type == CacheLevel.OPTION) {
                return; // don't invalidate for option changes
            }

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
            String rawDisplayName = parent.getRawDisplayName();
            return rawDisplayName.equals(getIdentifier()) ? Optional.empty() : Optional.of(rawDisplayName);
        }

        @Override
        public Optional<CommandSource> getCommandSource() {
            return Optional.empty();
        }

        @Override
        public LPSubjectCollection getParentCollection() {
            return plugin.getService().getGroupSubjects();
        }

        @Override
        public Subject sponge() {
            return ProxyFactory.toSponge(this);
        }

        @Override
        public LuckPermsService getService() {
            return plugin.getService();
        }

        @Override
        public Tristate getPermissionValue(ImmutableContextSet contexts, String permission) {
            NodeTree nt = permissionCache.get(contexts);
            Tristate t = CompatibilityUtil.convertTristate(nt.get(permission));
            if (t != Tristate.UNDEFINED) {
                return t;
            }

            t = plugin.getService().getGroupSubjects().getDefaults().getPermissionValue(contexts, permission);
            if (t != Tristate.UNDEFINED) {
                return t;
            }

            t = plugin.getService().getDefaults().getPermissionValue(contexts, permission);
            return t;
        }

        @Override
        public boolean isChildOf(ImmutableContextSet contexts, SubjectReference parent) {
            return parent.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP) && getPermissionValue(contexts, "group." + parent.getSubjectIdentifier()).asBoolean();
        }

        @Override
        public ImmutableList<SubjectReference> getParents(ImmutableContextSet contexts) {
            return parentCache.get(contexts);
        }

        @Override
        public Optional<String> getOption(ImmutableContextSet contexts, String s) {
            Optional<String> option;
            if (s.equalsIgnoreCase("prefix")) {
                option = getChatMeta(contexts, ChatMetaType.PREFIX);

            } else if (s.equalsIgnoreCase("suffix")) {
                option = getChatMeta(contexts, ChatMetaType.SUFFIX);

            } else {
                option = getMeta(contexts, s);
            }

            if (option.isPresent()) {
                return option;
            }

            option = plugin.getService().getGroupSubjects().getDefaults().getOption(contexts, s);
            if (option.isPresent()) {
                return option;
            }

            return plugin.getService().getDefaults().getOption(contexts, s);
        }

        @Override
        public ImmutableContextSet getActiveContextSet() {
            return plugin.getContextManager().getApplicableContext(this.sponge()).makeImmutable();
        }

        private Optional<String> getChatMeta(ImmutableContextSet contexts, ChatMetaType type) {
            MetaAccumulator metaAccumulator = parent.accumulateMeta(null, null, ExtractedContexts.generate(plugin.getService().calculateContexts(contexts)));
            return Optional.ofNullable(metaAccumulator.getStack(type).toFormattedString());
        }

        private Optional<String> getMeta(ImmutableContextSet contexts, String key) {
            MetaAccumulator metaAccumulator = parent.accumulateMeta(null, null, ExtractedContexts.generate(plugin.getService().calculateContexts(contexts)));
            ListMultimap<String, String> meta = metaAccumulator.getMeta();
            List<String> ret = meta.get(key);
            return ret.isEmpty() ? Optional.empty() : Optional.of(ret.get(0));
        }
    }
}
