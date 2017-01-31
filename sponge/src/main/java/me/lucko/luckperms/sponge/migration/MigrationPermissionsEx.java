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

package me.lucko.luckperms.sponge.migration;

import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectCollection;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

import static me.lucko.luckperms.sponge.migration.MigrationUtils.migrateSubject;

public class MigrationPermissionsEx extends SubCommand<Object> {
    public MigrationPermissionsEx() {
        super("permissionsex", "Migration from PermissionsEx", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        Consumer<String> log = s -> {
            Message.MIGRATION_LOG.send(sender, s);
            Message.MIGRATION_LOG.send(plugin.getConsoleSender(), s);
        };
        log.accept("Starting PermissionsEx migration.");
        
        final LuckPermsService lpService = ((LPSpongePlugin) plugin).getService();

        Optional<PluginContainer> pex = Sponge.getPluginManager().getPlugin("permissionsex");
        if (!pex.isPresent()) {
            log.accept("Error -> PermissionsEx is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        // Cast to PermissionService. PEX has all of it's damned classes defined as package private.
        PermissionService pexService = (PermissionService) pex.get().getInstance().get();

        // Migrate defaults
        for (SubjectCollection collection : pexService.getKnownSubjects().values()) {
            MigrationUtils.migrateSubjectData(
                    collection.getDefaults().getSubjectData(),
                    lpService.getSubjects("defaults").get(collection.getIdentifier()).getSubjectData()
            );
        }

        MigrationUtils.migrateSubjectData(pexService.getDefaults().getSubjectData(), lpService.getDefaults().getSubjectData());

        Map<String, TreeMap<Integer, String>> tracks = new HashMap<>();

        // Migrate groups
        log.accept("Starting group migration.");
        int groupCount = 0;
        for (Subject pexGroup : pexService.getGroupSubjects().getAllSubjects()) {
            groupCount++;

            String pexName = MigrationUtils.convertGroupName(pexGroup.getIdentifier().toLowerCase());

            // Make a LuckPerms group for the one being migrated
            plugin.getStorage().createAndLoadGroup(pexName).join();
            Group group = plugin.getGroupManager().getIfLoaded(pexName);
            migrateSubject(pexGroup, group);
            plugin.getStorage().saveGroup(group);

            // Pull track data
            Optional<String> track = pexGroup.getOption("rank-ladder");
            Optional<String> pos = pexGroup.getOption("rank");
            if (track.isPresent() && pos.isPresent()) {
                String trackName = track.get().toLowerCase();
                try {
                    int rank = Integer.parseInt(pos.get());
                    if (!tracks.containsKey(trackName)) {
                        tracks.put(trackName, new TreeMap<>(Comparator.reverseOrder()));
                    }

                    tracks.get(trackName).put(rank, pexName);
                } catch (NumberFormatException ignored) {}
            }

        }
        log.accept("Migrated " + groupCount + " groups");

        // Migrate tracks
        log.accept("Starting track migration.");
        for (Map.Entry<String, TreeMap<Integer, String>> e : tracks.entrySet()) {
            plugin.getStorage().createAndLoadTrack(e.getKey()).join();
            Track track = plugin.getTrackManager().getIfLoaded(e.getKey());
            for (String groupName : e.getValue().values()) {
                Group group = plugin.getGroupManager().getIfLoaded(groupName);
                if (group != null) {
                    try {
                        track.appendGroup(group);
                    } catch (ObjectAlreadyHasException ignored) {}
                }
            }
        }
        log.accept("Migrated " + tracks.size() + " tracks");

        // Migrate users
        log.accept("Starting user migration.");
        int userCount = 0;
        for (Subject pexUser : pexService.getUserSubjects().getAllSubjects()) {
            userCount++;
            UUID uuid = Util.parseUuid(pexUser.getIdentifier());
            if (uuid == null) {
                log.accept("Error -> Could not parse UUID for user: " + pexUser.getIdentifier());
                continue;
            }

            // Make a LuckPerms user for the one being migrated
            plugin.getStorage().loadUser(uuid, "null").join();
            User user = plugin.getUserManager().get(uuid);
            if (user.getNodes().size() <= 1) {
                user.clearNodes(false);
            }
            migrateSubject(pexUser, user);
            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);
        }

        log.accept("Migrated " + userCount + " users.");
        log.accept("Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
