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

package me.lucko.luckperms.sponge.service.proxy.api7;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.text.Text;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public final class PermissionDescriptionProxy implements PermissionDescription {
    private final LPPermissionService service;
    private final LPPermissionDescription handle;

    @Override
    public String getId() {
        return handle.getId();
    }

    @Override
    public Optional<Text> getDescription() {
        return handle.getDescription();
    }

    @Override
    public Optional<PluginContainer> getOwner() {
        return handle.getOwner();
    }

    @Override
    public Map<Subject, Boolean> getAssignedSubjects(String s) {
        return handle.getAssignedSubjects(s).entrySet().stream()
                .collect(ImmutableCollectors.toMap(
                        e -> new SubjectProxy(service, e.getKey().toReference()),
                        Map.Entry::getValue
                ));
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> findAssignedSubjects(String s) {
        return (CompletableFuture) handle.findAssignedSubjects(s);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof PermissionDescriptionProxy && handle.equals(((PermissionDescriptionProxy) o).handle);
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public String toString() {
        return "luckperms.api7.PermissionDescriptionProxy(handle=" + this.handle + ")";
    }
}
