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

package me.lucko.luckperms.sponge.service.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * LuckPerms model for the Sponge {@link PermissionService}
 */
public interface LPPermissionService {

    LuckPermsPlugin getPlugin();

    PermissionService sponge();

    LPSubjectCollection getUserSubjects();

    LPSubjectCollection getGroupSubjects();

    LPSubjectCollection getDefaultSubjects();

    default LPSubjectData getDefaultData() {
        return getDefaults().getSubjectData();
    }

    LPSubject getDefaults();

    Predicate<String> getIdentifierValidityPredicate();

    LPSubjectCollection getCollection(String identifier);

    ImmutableMap<String, LPSubjectCollection> getLoadedCollections();

    SubjectReference newSubjectReference(String collectionIdentifier, String subjectIdentifier);

    LPPermissionDescription registerPermissionDescription(String id, Text description, PluginContainer owner);

    Optional<LPPermissionDescription> getDescription(String permission);

    ImmutableCollection<LPPermissionDescription> getDescriptions();

    void registerContextCalculator(ContextCalculator<Subject> calculator);

    // utils
    ImmutableList<SubjectReference> sortSubjects(Collection<SubjectReference> s);

    Contexts calculateContexts(ImmutableContextSet contextSet);

    void invalidatePermissionCaches();

    void invalidateParentCaches();

    void invalidateOptionCaches();
}
