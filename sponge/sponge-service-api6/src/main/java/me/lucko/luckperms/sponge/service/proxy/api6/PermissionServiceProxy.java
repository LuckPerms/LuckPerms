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

package me.lucko.luckperms.sponge.service.proxy.api6;

import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

public final class PermissionServiceProxy implements PermissionService {
    private final LPPermissionService handle;

    public PermissionServiceProxy(LPPermissionService handle) {
        this.handle = handle;
    }

    @Nonnull
    @Override
    public SubjectCollection getUserSubjects() {
        return this.handle.getUserSubjects().sponge();
    }

    @Nonnull
    @Override
    public SubjectCollection getGroupSubjects() {
        return this.handle.getGroupSubjects().sponge();
    }

    @Nonnull
    @Override
    public Subject getDefaults() {
        return this.handle.getRootDefaults().sponge();
    }

    @Nonnull
    @Override
    public SubjectCollection getSubjects(@Nonnull String s) {
        return this.handle.getCollection(s).sponge();
    }

    @Nonnull
    @Override
    public Map<String, SubjectCollection> getKnownSubjects() {
        return this.handle.getLoadedCollections().entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().sponge()
                ));
    }

    @Override
    public Optional<PermissionDescription.Builder> newDescriptionBuilder(@Nonnull Object o) {
        Optional<PluginContainer> container = Sponge.getGame().getPluginManager().fromInstance(o);
        if (!container.isPresent()) {
            throw new IllegalArgumentException("Couldn't find a plugin container for " + o.getClass().getSimpleName());
        }

        return Optional.of(new DescriptionBuilder(this.handle, container.get()));
    }

    @Nonnull
    @Override
    public Optional<PermissionDescription> getDescription(@Nonnull String s) {
        return this.handle.getDescription(s).map(LPPermissionDescription::sponge);
    }

    @Nonnull
    @Override
    public Collection<PermissionDescription> getDescriptions() {
        return this.handle.getDescriptions().stream().map(LPPermissionDescription::sponge).collect(ImmutableCollectors.toSet());
    }

    @Override
    public void registerContextCalculator(@Nonnull ContextCalculator<Subject> contextCalculator) {
        this.handle.registerContextCalculator(contextCalculator);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof PermissionServiceProxy && this.handle.equals(((PermissionServiceProxy) o).handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api6.PermissionServiceProxy(handle=" + this.handle + ")";
    }
}
