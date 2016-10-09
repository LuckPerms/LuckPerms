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

package me.lucko.luckperms.api.sponge;

import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.event.events.UserPermissionRefreshEvent;
import me.lucko.luckperms.api.implementation.internal.UserLink;
import me.lucko.luckperms.users.User;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.util.Tristate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class LuckPermsUserSubject extends LuckPermsSubject {
    public static LuckPermsUserSubject wrapUser(User user, LuckPermsService service) {
        return new LuckPermsUserSubject(user, service);
    }

    @Getter
    private User user;

    @Getter
    private Map<Map<String, String>, ContextData> contextData;

    private LuckPermsUserSubject(User user, LuckPermsService service) {
        super(user, service);
        this.user = user;

        contextData = new ConcurrentHashMap<>();
    }

    @Override
    public void deprovision() {
        /* For some reason, Sponge holds onto User instances in a cache, which in turn, prevents LuckPerms data from being GCed.
           As well as unloading, we also remove all references to the User instances. */
        super.deprovision();
        user = null;
        contextData = null;
    }

    @Override
    public Tristate getPermissionValue(@NonNull Set<Context> contexts, @NonNull String permission) {
        Map<String, String> context = contexts.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));
        ContextData cd = contextData.computeIfAbsent(context, map -> calculatePermissions(map, false));

        me.lucko.luckperms.api.Tristate t =  cd.getPermissionValue(permission);
        if (t != me.lucko.luckperms.api.Tristate.UNDEFINED) {
            return Tristate.fromBoolean(t.asBoolean());
        } else {
            return Tristate.UNDEFINED;
        }
    }

    public ContextData calculatePermissions(Map<String, String> context, boolean apply) {
        Map<String, Boolean> toApply = user.exportNodes(
                new Contexts(
                        context,
                        service.getPlugin().getConfiguration().isIncludingGlobalPerms(),
                        service.getPlugin().getConfiguration().isIncludingGlobalWorldPerms(),
                        true,
                        service.getPlugin().getConfiguration().isApplyingGlobalGroups(),
                        service.getPlugin().getConfiguration().isApplyingGlobalWorldGroups()
                ),
                Collections.emptyList(),
                true
        );

        ContextData existing = contextData.get(context);
        if (existing == null) {
            existing = new ContextData(this, context, service);
            if (apply) {
                contextData.put(context, existing);
            }
        }

        boolean different = false;
        if (toApply.size() != existing.getPermissionCache().size()) {
            different = true;
        } else {
            for (Map.Entry<String, Boolean> e : existing.getPermissionCache().entrySet()) {
                if (toApply.containsKey(e.getKey()) && toApply.get(e.getKey()) == e.getValue()) {
                    continue;
                }
                different = true;
                break;
            }
        }

        if (!different) return existing;

        existing.getPermissionCache().clear();
        existing.invalidateCache();
        existing.getPermissionCache().putAll(toApply);
        service.getPlugin().getApiProvider().fireEventAsync(new UserPermissionRefreshEvent(new UserLink(user)));
        return existing;
    }

    public void calculatePermissions(Set<Context> contexts, boolean apply) {
        Map<String, String> context = contexts.stream().collect(Collectors.toMap(Context::getKey, Context::getValue));
        calculatePermissions(context, apply);
    }

    public void calculateActivePermissions(boolean apply) {
        calculatePermissions(getActiveContexts(), apply);
    }


    @Override
    public String getIdentifier() {
        return service.getPlugin().getUuidCache().getExternalUUID(user.getUuid()).toString();
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        final UUID uuid = service.getPlugin().getUuidCache().getExternalUUID(user.getUuid());

        Optional<Player> p = Sponge.getServer().getPlayer(uuid);
        if (p.isPresent()) {
            return Optional.of(p.get());
        }

        return Optional.empty();
    }

    @Override
    public Set<Context> getActiveContexts() {
        final UUID uuid = service.getPlugin().getUuidCache().getExternalUUID(user.getUuid());
        Optional<Player> player = Sponge.getServer().getPlayer(uuid);

        if (!player.isPresent()) {
            return SubjectData.GLOBAL_CONTEXT;
        }

        Map<String, String> context = new HashMap<>();
        service.getPlugin().getContextManager().giveApplicableContext(player.get(), context);
        return context.entrySet().stream().map(e -> new Context(e.getKey(), e.getValue())).collect(Collectors.toSet());
    }
}
