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

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

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

@ToString(of = {"container", "roles", "id", "description"})
@EqualsAndHashCode(of = {"container", "roles", "id", "description"})
@RequiredArgsConstructor
public final class SimpleDescriptionBuilder implements PermissionDescription.Builder {
    private final LPPermissionService service;
    private final PluginContainer container;
    private final Map<String, Tristate> roles = new HashMap<>();
    private String id = null;
    private Text description = null;

    @Override
    public PermissionDescription.Builder id(@NonNull String s) {
        id = s;
        return this;
    }

    @Override
    public PermissionDescription.Builder description(Text text) {
        description = text;
        return this;
    }

    @Override
    public PermissionDescription.Builder assign(@NonNull String s, boolean b) {
        roles.put(s, Tristate.fromBoolean(b));
        return this;
    }

    @Override
    public PermissionDescription register() throws IllegalStateException {
        if (id == null) {
            throw new IllegalStateException("id cannot be null");
        }

        LPPermissionDescription d = service.registerPermissionDescription(id, description, container);

        // Set role-templates
        LPSubjectCollection subjects = service.getCollection(PermissionService.SUBJECTS_ROLE_TEMPLATE);
        for (Map.Entry<String, Tristate> assignment : roles.entrySet()) {
            LPSubject subject = subjects.loadSubject(assignment.getKey()).join();
            subject.getTransientSubjectData().setPermission(ContextSet.empty(), id, assignment.getValue());
        }

        service.getPlugin().getPermissionVault().offer(id);

        // null stuff so this instance can be reused
        roles.clear();
        id = null;
        description = null;

        return d.sponge();
    }
}
