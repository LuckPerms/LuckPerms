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

package me.lucko.luckperms.sponge.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "description", "owner"})
public final class LuckPermsPermissionDescription implements LPPermissionDescription {

    @Getter
    private final LPPermissionService service;

    @Getter
    private final String id;

    private final Text description;

    private final PluginContainer owner;

    private PermissionDescription spongeProxy = null;

    @Override
    public synchronized PermissionDescription sponge() {
        if (spongeProxy == null) {
            spongeProxy = ProxyFactory.toSponge(this);
        }
        return spongeProxy;
    }

    @Override
    public Optional<Text> getDescription() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<PluginContainer> getOwner() {
        return Optional.ofNullable(owner);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> findAssignedSubjects(String id) {
        LPSubjectCollection collection = service.getCollection(id);
        return (CompletableFuture) collection.getAllWithPermission(this.id);
    }

    @Override
    public Map<LPSubject, Boolean> getAssignedSubjects(String id) {
        LPSubjectCollection collection = service.getCollection(id);
        return collection.getLoadedWithPermission(this.id);
    }
}
