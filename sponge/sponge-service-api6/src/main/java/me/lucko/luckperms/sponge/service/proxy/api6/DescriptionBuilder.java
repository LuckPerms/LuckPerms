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

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.sponge.service.model.LPPermissionDescription;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectCollection;

import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DescriptionBuilder implements PermissionDescription.Builder {
    @Nonnull private final LPPermissionService service;
    @Nonnull private final PluginContainer container;
    @Nonnull private final Map<String, Tristate> roles = new HashMap<>();
    @Nullable private String id = null;
    @Nullable private Text description = null;

    public DescriptionBuilder(@Nonnull LPPermissionService service, @Nonnull PluginContainer container) {
        this.service = Objects.requireNonNull(service, "service");
        this.container = Objects.requireNonNull(container, "container");
    }

    @Nonnull
    @Override
    public PermissionDescription.Builder id(@Nonnull String id) {
        this.id = Objects.requireNonNull(id, "id");
        return this;
    }

    @Nonnull
    @Override
    public PermissionDescription.Builder description(@Nonnull Text description) {
        this.description = Objects.requireNonNull(description, "description");
        return this;
    }

    @Nonnull
    @Override
    public PermissionDescription.Builder assign(@Nonnull String role, boolean value) {
        Objects.requireNonNull(role, "role");
        this.roles.put(role, Tristate.fromBoolean(value));
        return this;
    }

    @Nonnull
    @Override
    public PermissionDescription register() throws IllegalStateException {
        if (this.id == null) {
            throw new IllegalStateException("id cannot be null");
        }

        LPPermissionDescription description = this.service.registerPermissionDescription(this.id, this.description, this.container);

        // Set role-templates
        LPSubjectCollection subjects = this.service.getCollection(PermissionService.SUBJECTS_ROLE_TEMPLATE);
        for (Map.Entry<String, Tristate> assignment : this.roles.entrySet()) {
            LPSubject roleSubject = subjects.loadSubject(assignment.getKey()).join();
            roleSubject.getTransientSubjectData().setPermission(ContextSet.empty(), this.id, assignment.getValue());
        }

        this.service.getPlugin().getPermissionRegistry().offer(this.id);

        // null stuff so this instance can be reused
        this.roles.clear();
        this.id = null;
        this.description = null;

        return description.sponge();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof DescriptionBuilder)) return false;
        final DescriptionBuilder other = (DescriptionBuilder) o;

        return this.container.equals(other.container) &&
                this.roles.equals(other.roles) &&
                (this.id == null ? other.id == null : this.id.equals(other.id)) &&
                (this.description == null ? other.description == null : this.description.equals(other.description));
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.container.hashCode();
        result = result * PRIME + this.roles.hashCode();
        result = result * PRIME + (this.id == null ? 43 : this.id.hashCode());
        result = result * PRIME + (this.description == null ? 43 : this.description.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "SimpleDescriptionBuilder(" +
                "container=" + this.container + ", " +
                "roles=" + this.roles + ", " +
                "id=" + this.id + ", " +
                "description=" + this.description + ")";
    }
}
