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

package me.lucko.luckperms.sponge.managers;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.managers.group.AbstractGroupManager;
import me.lucko.luckperms.common.storage.DataConstraints;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.model.SpongeGroup;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.reference.LPSubjectReference;
import me.lucko.luckperms.sponge.service.reference.SubjectReferenceFactory;

import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class SpongeGroupManager extends AbstractGroupManager<SpongeGroup> implements LPSubjectCollection {
    private final LPSpongePlugin plugin;
    private SubjectCollection spongeProxy = null;

    private final LoadingCache<String, LPSubject> subjectLoadingCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build(s -> {
                SpongeGroup group = getIfLoaded(s);
                if (group != null) {
                    // they're already loaded, but the data might not actually be there yet
                    // if stuff is being loaded, then the user's i/o lock will be locked by the storage impl
                    group.getIoLock().lock();
                    group.getIoLock().unlock();

                    return group.sponge();
                }

                // Request load
                getPlugin().getStorage().createAndLoadGroup(s, CreationCause.INTERNAL).join();

                group = getIfLoaded(s);
                if (group == null) {
                    getPlugin().getLog().severe("Error whilst loading group '" + s + "'.");
                    throw new RuntimeException();
                }

                return group.sponge();
            });

    public SpongeGroupManager(LPSpongePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public SpongeGroup apply(String name) {
        return new SpongeGroup(name, this.plugin);
    }

    @Override
    public synchronized SubjectCollection sponge() {
        if (this.spongeProxy == null) {
            Objects.requireNonNull(this.plugin.getService(), "service");
            this.spongeProxy = ProxyFactory.toSponge(this);
        }
        return this.spongeProxy;
    }

    public LPSpongePlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public LuckPermsService getService() {
        return this.plugin.getService();
    }

    @Override
    public String getIdentifier() {
        return PermissionService.SUBJECTS_GROUP;
    }

    @Override
    public Predicate<String> getIdentifierValidityPredicate() {
        return DataConstraints.GROUP_NAME_TEST;
    }

    @Override
    public CompletableFuture<LPSubject> loadSubject(String identifier) {
        if (!DataConstraints.GROUP_NAME_TEST.test(identifier)) {
            throw new IllegalArgumentException("Illegal subject identifier");
        }

        LPSubject present = this.subjectLoadingCache.getIfPresent(identifier.toLowerCase());
        if (present != null) {
            return CompletableFuture.completedFuture(present);
        }

        return CompletableFuture.supplyAsync(() -> this.subjectLoadingCache.get(identifier.toLowerCase()), this.plugin.getScheduler().async());
    }

    @Override
    public Optional<LPSubject> getSubject(String identifier) {
        if (!DataConstraints.GROUP_NAME_TEST.test(identifier)) {
            return Optional.empty();
        }

        return Optional.ofNullable(getIfLoaded(identifier.toLowerCase())).map(SpongeGroup::sponge);
    }

    @Override
    public CompletableFuture<Boolean> hasRegistered(String identifier) {
        if (!DataConstraints.GROUP_NAME_TEST.test(identifier)) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(isLoaded(identifier.toLowerCase()));
    }

    @Override
    public CompletableFuture<ImmutableCollection<LPSubject>> loadSubjects(Set<String> identifiers) {
        return CompletableFuture.supplyAsync(() -> {
            ImmutableSet.Builder<LPSubject> ret = ImmutableSet.builder();
            for (String id : identifiers) {
                if (!DataConstraints.GROUP_NAME_TEST.test(id)) {
                    continue;
                }
                ret.add(loadSubject(id.toLowerCase()).join());
            }

            return ret.build();
        }, this.plugin.getScheduler().async());
    }

    @Override
    public ImmutableCollection<LPSubject> getLoadedSubjects() {
        return getAll().values().stream().map(SpongeGroup::sponge).collect(ImmutableCollectors.toSet());
    }

    @Override
    public CompletableFuture<ImmutableSet<String>> getAllIdentifiers() {
        return CompletableFuture.completedFuture(ImmutableSet.copyOf(getAll().keySet()));
    }

    @Override
    public CompletableFuture<ImmutableMap<LPSubjectReference, Boolean>> getAllWithPermission(String permission) {
        return CompletableFuture.supplyAsync(() -> {
            ImmutableMap.Builder<LPSubjectReference, Boolean> ret = ImmutableMap.builder();

            List<HeldPermission<String>> lookup = this.plugin.getStorage().getGroupsWithPermission(permission).join();
            for (HeldPermission<String> holder : lookup) {
                if (holder.asNode().getFullContexts().equals(ImmutableContextSet.empty())) {
                    ret.put(SubjectReferenceFactory.obtain(getService(), getIdentifier(), holder.getHolder()), holder.getValue());
                }
            }

            return ret.build();
        }, this.plugin.getScheduler().async());
    }

    @Override
    public CompletableFuture<ImmutableMap<LPSubjectReference, Boolean>> getAllWithPermission(ImmutableContextSet contexts, String permission) {
        return CompletableFuture.supplyAsync(() -> {
            ImmutableMap.Builder<LPSubjectReference, Boolean> ret = ImmutableMap.builder();

            List<HeldPermission<String>> lookup = this.plugin.getStorage().getGroupsWithPermission(permission).join();
            for (HeldPermission<String> holder : lookup) {
                if (holder.asNode().getFullContexts().equals(contexts)) {
                    ret.put(SubjectReferenceFactory.obtain(getService(), getIdentifier(), holder.getHolder()), holder.getValue());
                }
            }

            return ret.build();
        }, this.plugin.getScheduler().async());
    }

    @Override
    public ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(String permission) {
        return getAll().values().stream()
                .map(SpongeGroup::sponge)
                .map(sub -> Maps.immutableEntry(sub, sub.getPermissionValue(ImmutableContextSet.empty(), permission)))
                .filter(pair -> pair.getValue() != Tristate.UNDEFINED)
                .collect(ImmutableCollectors.toMap(Map.Entry::getKey, sub -> sub.getValue().asBoolean()));
    }

    @Override
    public ImmutableMap<LPSubject, Boolean> getLoadedWithPermission(ImmutableContextSet contexts, String permission) {
        return getAll().values().stream()
                .map(SpongeGroup::sponge)
                .map(sub -> Maps.immutableEntry(sub, sub.getPermissionValue(contexts, permission)))
                .filter(pair -> pair.getValue() != Tristate.UNDEFINED)
                .collect(ImmutableCollectors.toMap(Map.Entry::getKey, sub -> sub.getValue().asBoolean()));
    }

    @Override
    public LPSubject getDefaults() {
        return getService().getDefaultSubjects().loadSubject(getIdentifier()).join();
    }

}
