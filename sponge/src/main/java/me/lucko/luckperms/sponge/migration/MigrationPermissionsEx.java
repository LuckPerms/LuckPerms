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

package me.lucko.luckperms.sponge.migration;

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.logging.ProgressLogger;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.SafeIterator;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import me.lucko.luckperms.sponge.service.LuckPermsService;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static me.lucko.luckperms.sponge.migration.SpongeMigrationUtils.migrateSubject;
import static me.lucko.luckperms.sponge.migration.SpongeMigrationUtils.migrateSubjectData;

public class MigrationPermissionsEx extends SubCommand<Object> {
    public MigrationPermissionsEx(LocaleManager locale) {
        super(CommandSpec.MIGRATION_COMMAND.spec(locale), "permissionsex", CommandPermission.MIGRATION, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        ProgressLogger log = new ProgressLogger("PermissionsEx");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");
        
        final LuckPermsService lpService = ((LPSpongePlugin) plugin).getService();

        Optional<PluginContainer> pex = Sponge.getPluginManager().getPlugin("permissionsex");
        if (!pex.isPresent()) {
            log.logErr("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        // Cast to PermissionService. PEX has all of it's damned classes defined as package private.
        PermissionService pexService = (PermissionService) pex.get().getInstance().get();

        // Migrate defaults
        log.log("Migrating default subjects.");
        SafeIterator.iterate(pexService.getKnownSubjects().values(), collection -> {
            migrateSubjectData(
                    collection.getDefaults().getSubjectData(),
                    lpService.getCollection("defaults").loadSubject(collection.getIdentifier()).join().sponge().getSubjectData()
            );
        });
        migrateSubjectData(pexService.getDefaults().getSubjectData(), lpService.getDefaults().sponge().getSubjectData());

        log.log("Calculating group weightings.");
        int i = 0;
        for (Subject pexGroup : pexService.getGroupSubjects().getAllSubjects()) {
            Optional<String> pos = pexGroup.getOption("rank");
            if (pos.isPresent()) {
                try {
                    i = Math.max(i, Integer.parseInt(pos.get()));
                } catch (NumberFormatException ignored) {}
            }
        }
        int maxWeight = i + 5;

        Map<String, TreeMap<Integer, String>> tracks = new HashMap<>();

        // Migrate groups
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        SafeIterator.iterate(pexService.getGroupSubjects().getAllSubjects(), pexGroup -> {
            String pexName = MigrationUtils.standardizeName(pexGroup.getIdentifier());

            Optional<String> rankString = pexGroup.getOption("rank");
            OptionalInt rank = OptionalInt.empty();
            if (rankString.isPresent()) {
                try {
                    int r = Integer.parseInt(rankString.get());
                    rank = OptionalInt.of(r);
                } catch (NumberFormatException ignored) {}
            }

            int weight = 100;
            if (rank.isPresent()) {
                weight = maxWeight - rank.getAsInt();
            }

            // Make a LuckPerms group for the one being migrated
            plugin.getStorage().createAndLoadGroup(pexName, CreationCause.INTERNAL).join();
            Group group = plugin.getGroupManager().getIfLoaded(pexName);
            migrateSubject(pexGroup, group, weight);
            plugin.getStorage().saveGroup(group);

            // Pull track data
            Optional<String> track = pexGroup.getOption("rank-ladder");
            if (track.isPresent() && rank.isPresent()) {
                String trackName = MigrationUtils.standardizeName(track.get());
                if (!tracks.containsKey(trackName)) {
                    tracks.put(trackName, new TreeMap<>(Comparator.reverseOrder()));
                }
                tracks.get(trackName).put(rank.getAsInt(), pexName);
            }

            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        });
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate tracks
        log.log("Starting track migration.");
        SafeIterator.iterate(tracks.entrySet(), e -> {
            plugin.getStorage().createAndLoadTrack(e.getKey(), CreationCause.INTERNAL).join();
            Track track = plugin.getTrackManager().getIfLoaded(e.getKey());
            for (String groupName : e.getValue().values()) {
                Group group = plugin.getGroupManager().getIfLoaded(groupName);
                if (group != null) {
                    track.appendGroup(group);
                }
            }
        });
        log.log("Migrated " + tracks.size() + " tracks");

        // Migrate users
        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);

        // Increment the max weight from the group migrations. All user meta should override.
        int userWeight = maxWeight + 5;

        SafeIterator.iterate(pexService.getUserSubjects().getAllSubjects(), pexUser -> {
            UUID uuid = CommandUtils.parseUuid(pexUser.getIdentifier());
            if (uuid == null) {
                log.logErr("Could not parse UUID for user: " + pexUser.getIdentifier());
                return;
            }

            // Make a LuckPerms user for the one being migrated
            plugin.getStorage().loadUser(uuid, null).join();
            User user = plugin.getUserManager().getIfLoaded(uuid);
            if (user.getEnduringNodes().size() <= 1) {
                user.clearNodes(false);
            }
            migrateSubject(pexUser, user, userWeight);
            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);

            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }
}
