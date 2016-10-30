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

package me.lucko.luckperms.sponge.service.persisted;

import co.aikar.timings.Timing;
import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.common.utils.BufferedRequest;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.timings.LPTiming;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.MemorySubjectData;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;
import org.spongepowered.api.util.Tristate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A simple persistable Subject implementation
 */
@Getter
public class PersistedSubject implements Subject {
    private final String identifier;

    private final LuckPermsService service;
    private final SubjectCollection containingCollection;
    private final PersistedSubjectData subjectData;
    private final MemorySubjectData transientSubjectData;
    private final BufferedRequest<Void> saveBuffer = new BufferedRequest<Void>(1000L, r -> PersistedSubject.this.service.getPlugin().doAsync(r)) {
        @Override
        protected Void perform() {
            service.getPlugin().doAsync(() -> {
                try {
                    service.getStorage().saveToFile(PersistedSubject.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return null;
        }
    };

    public PersistedSubject(String identifier, LuckPermsService service, SubjectCollection containingCollection) {
        this.identifier = identifier;
        this.service = service;
        this.containingCollection = containingCollection;
        this.subjectData = new PersistedSubjectData(service, this);
        this.transientSubjectData = new MemorySubjectData(service);
    }

    public void loadData(SubjectDataHolder dataHolder) {
        subjectData.setSave(false);
        dataHolder.copyTo(subjectData, service);
        subjectData.setSave(true);
    }

    public void save() {
        saveBuffer.request();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }

    @Override
    public boolean hasPermission(@NonNull Set<Context> contexts, @NonNull String node) {
        return getPermissionValue(contexts, node).asBoolean();
    }

    @Override
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String node) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.PERSISTED_SUBJECT_GET_PERMISSION_VALUE)) {
            Tristate res = subjectData.getNodeTree(contexts).get(node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }

            res = transientSubjectData.getNodeTree(contexts).get(node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }

            for (Subject parent : getParents(contexts)) {
                Tristate tempRes = parent.getPermissionValue(contexts, node);
                if (tempRes != Tristate.UNDEFINED) {
                    return tempRes;
                }
            }

            if (getContainingCollection().getIdentifier().equalsIgnoreCase("defaults")) {
                return Tristate.UNDEFINED;
            }

            res = service.getGroupSubjects().getDefaults().getPermissionValue(contexts, node);
            if (res != Tristate.UNDEFINED) {
                return res;
            }

            res = service.getDefaults().getPermissionValue(contexts, node);
            return res;
        }
    }

    @Override
    public boolean isChildOf(@NonNull Set<Context> contexts, @NonNull Subject subject) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.PERSISTED_SUBJECT_IS_CHILD_OF)) {
            if (getContainingCollection().getIdentifier().equalsIgnoreCase("defaults")) {
                return subjectData.getParents(contexts).contains(subject) ||
                        transientSubjectData.getParents(contexts).contains(subject);
            } else {
                return subjectData.getParents(contexts).contains(subject) ||
                        transientSubjectData.getParents(contexts).contains(subject) ||
                        getContainingCollection().getDefaults().getParents(contexts).contains(subject) ||
                        service.getDefaults().getParents(contexts).contains(subject);
            }
        }
    }

    @Override
    public List<Subject> getParents(@NonNull Set<Context> contexts) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.PERSISTED_SUBJECT_GET_PARENTS)) {
            List<Subject> s = new ArrayList<>();
            s.addAll(subjectData.getParents(contexts));
            s.addAll(transientSubjectData.getParents(contexts));

            if (!getContainingCollection().getIdentifier().equalsIgnoreCase("defaults")) {
                s.addAll(getContainingCollection().getDefaults().getParents(contexts));
                s.addAll(service.getDefaults().getParents(contexts));
            }

            return ImmutableList.copyOf(s);
        }
    }

    @Override
    public Optional<String> getOption(Set<Context> set, String key) {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.PERSISTED_SUBJECT_GET_OPTION)) {
            Optional<String> res = Optional.ofNullable(subjectData.getOptions(getActiveContexts()).get(key));
            if (res.isPresent()) {
                return res;
            }

            res = Optional.ofNullable(transientSubjectData.getOptions(getActiveContexts()).get(key));
            if (res.isPresent()) {
                return res;
            }

            for (Subject parent : getParents(getActiveContexts())) {
                Optional<String> tempRes = parent.getOption(getActiveContexts(), key);
                if (tempRes.isPresent()) {
                    return tempRes;
                }
            }

            if (getContainingCollection().getIdentifier().equalsIgnoreCase("defaults")) {
                return Optional.empty();
            }

            res = getContainingCollection().getDefaults().getOption(set, key);
            if (res.isPresent()) {
                return res;
            }

            return service.getDefaults().getOption(set, key);
        }
    }

    @Override
    public Set<Context> getActiveContexts() {
        try (Timing ignored = service.getPlugin().getTimings().time(LPTiming.PERSISTED_SUBJECT_GET_ACTIVE_CONTEXTS)) {
            return LuckPermsService.convertContexts(service.getPlugin().getContextManager().getApplicableContext(this));
        }
    }
}
