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

package me.lucko.luckperms.bukkit.migration;

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.ProgressLogger;

import org.bukkit.Bukkit;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import org.tyrannyofheaven.bukkit.zPermissions.dao.PermissionService;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Inheritance;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationZPermissions extends SubCommand<Object> {
    public MigrationZPermissions() {
        super("zpermissions", "Migration from zPermissions", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        ProgressLogger log = new ProgressLogger("zPermissions");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");
        
        if (!Bukkit.getPluginManager().isPluginEnabled("zPermissions")) {
            log.logErr("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        if (!Bukkit.getServicesManager().isProvidedFor(ZPermissionsService.class)) {
            log.logErr("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        ZPermissionsService service = Bukkit.getServicesManager().getRegistration(ZPermissionsService.class).getProvider();

        PermissionService internalService;
        try {
            Field psField = service.getClass().getDeclaredField("permissionService");
            psField.setAccessible(true);
            internalService = (PermissionService) psField.get(service);
        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.FAILURE;
        }

        // Migrate all groups
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        int maxWeight = 0;
        for (String g : service.getAllGroups()) {
            PermissionEntity entity = internalService.getEntity(g, null, true);

            String groupName = MigrationUtils.standardizeName(g);
            plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();
            Group group = plugin.getGroupManager().getIfLoaded(groupName);

            int weight = entity.getPriority();
            maxWeight = Math.max(maxWeight, weight);
            migrateEntity(group, entity, null, weight);
            MigrationUtils.setGroupWeight(group, weight);

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        }
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate all tracks
        log.log("Starting track migration.");
        AtomicInteger trackCount = new AtomicInteger(0);
        for (String t : service.getAllTracks()) {
            String trackName = MigrationUtils.standardizeName(t);

            plugin.getStorage().createAndLoadTrack(trackName, CreationCause.INTERNAL).join();
            Track track = plugin.getTrackManager().getIfLoaded(trackName);
            track.setGroups(service.getTrackGroups(t));
            plugin.getStorage().saveTrack(track);

            log.logAllProgress("Migrated {} tracks so far.", trackCount.incrementAndGet());
        }
        log.log("Migrated " + trackCount.get() + " tracks");

        // Migrate all users.
        log.log("Starting user migration.");
        maxWeight += 10;
        AtomicInteger userCount = new AtomicInteger(0);
        for (UUID u : service.getAllPlayersUUID()) {
            PermissionEntity entity = internalService.getEntity(null, u, false);
            String username = null;
            if (!entity.isGroup()) {
                username = entity.getDisplayName();
            }

            plugin.getStorage().loadUser(u, username).join();
            User user = plugin.getUserManager().getIfLoaded(u);
            migrateEntity(user, entity, internalService.getGroups(u), maxWeight);
            user.getPrimaryGroup().setStoredValue(MigrationUtils.standardizeName(service.getPlayerPrimaryGroup(u)));

            plugin.getUserManager().cleanup(user);
            plugin.getStorage().saveUser(user);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        }

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }

    private void migrateEntity(PermissionHolder holder, PermissionEntity entity, List<Membership> memberships, int weight) {
        for (Entry e : entity.getPermissions()) {
            if (e.getWorld() != null && !e.getWorld().getName().equals("")) {
                holder.setPermission(NodeFactory.newBuilder(e.getPermission()).setValue(e.isValue()).setWorld(e.getWorld().getName()).build());
            } else {
                holder.setPermission(NodeFactory.newBuilder(e.getPermission()).setValue(e.isValue()).build());
            }
        }

        if (entity.isGroup()) {
            // entity.getMemberships() doesn't work for groups (always returns 0 records)
            for (Inheritance inheritance : entity.getInheritancesAsChild()) {
                if (!inheritance.getParent().getName().equals(holder.getObjectName())) {
                    holder.setPermission(NodeFactory.make("group." + MigrationUtils.standardizeName(inheritance.getParent().getName())));
                }
            }
        } else {
            for (Membership membership : memberships) {
                holder.setPermission(NodeFactory.make("group." + MigrationUtils.standardizeName(membership.getGroup().getDisplayName())));
            }
        }

        for (EntityMetadata metadata : entity.getMetadata()) {
            String key = metadata.getName().toLowerCase();

            if (key.equals("prefix") || key.equals("suffix")) {
                holder.setPermission(NodeFactory.makeChatMetaNode(key.equals("prefix"), weight, metadata.getStringValue()).build());
            } else {
                holder.setPermission(NodeFactory.makeMetaNode(key, metadata.getStringValue()).build());
            }
        }
    }
}
