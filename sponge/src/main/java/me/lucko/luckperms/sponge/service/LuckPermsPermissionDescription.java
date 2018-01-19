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

import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;
import me.lucko.luckperms.sponge.service.reference.LPSubjectReference;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

public final class LuckPermsPermissionDescription implements LPPermissionDescription {

    private final LPPermissionService service;

    private final String id;
    @Nullable private final Text description;
    @Nullable private final PluginContainer owner;

    private PermissionDescription spongeProxy = null;

    public LuckPermsPermissionDescription(LPPermissionService service, String id, @Nullable Text description, @Nullable PluginContainer owner) {
        this.service = service;
        this.id = Objects.requireNonNull(id, "id");
        this.description = description;
        this.owner = owner;
    }

    @Override
    public synchronized PermissionDescription sponge() {
        if (this.spongeProxy == null) {
            this.spongeProxy = ProxyFactory.toSponge(this);
        }
        return this.spongeProxy;
    }

    @Override
    public LPPermissionService getService() {
        return this.service;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public Optional<Text> getDescription() {
        return Optional.ofNullable(this.description);
    }

    @Override
    public Optional<PluginContainer> getOwner() {
        return Optional.ofNullable(this.owner);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Map<LPSubjectReference, Boolean>> findAssignedSubjects(String id) {
        LPSubjectCollection collection = this.service.getCollection(id);
        return (CompletableFuture) collection.getAllWithPermission(this.id);
    }

    @Override
    public Map<LPSubject, Boolean> getAssignedSubjects(String id) {
        LPSubjectCollection collection = this.service.getCollection(id);
        return collection.getLoadedWithPermission(this.id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LuckPermsPermissionDescription)) return false;
        final LuckPermsPermissionDescription other = (LuckPermsPermissionDescription) o;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "LuckPermsPermissionDescription(" +
                "id=" + this.id + ", " +
                "description=" + this.description + ", " +
                "owner=" + this.owner + ")";
    }

}
