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

package me.lucko.luckperms.common.commands.misc;

import com.github.benmanes.caffeine.cache.Cache;

import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.BulkUpdateBuilder;
import me.lucko.luckperms.common.bulkupdate.DataType;
import me.lucko.luckperms.common.bulkupdate.action.DeleteAction;
import me.lucko.luckperms.common.bulkupdate.action.UpdateAction;
import me.lucko.luckperms.common.bulkupdate.comparison.Comparison;
import me.lucko.luckperms.common.bulkupdate.comparison.Constraint;
import me.lucko.luckperms.common.bulkupdate.comparison.StandardComparison;
import me.lucko.luckperms.common.bulkupdate.query.Query;
import me.lucko.luckperms.common.bulkupdate.query.QueryField;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.ArgumentParser;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.CaffeineFactory;
import me.lucko.luckperms.common.util.Predicates;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BulkUpdateCommand extends SingleCommand {
    private final Cache<String, BulkUpdate> pendingOperations = CaffeineFactory.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    public BulkUpdateCommand(LocaleManager locale) {
        super(CommandSpec.BULK_UPDATE.localize(locale), "BulkUpdate", CommandPermission.BULK_UPDATE, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException {
        if (!sender.isConsole()) {
            Message.BULK_UPDATE_MUST_USE_CONSOLE.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        if (args.size() == 2 && args.get(0).equalsIgnoreCase("confirm")) {
            String id = args.get(1);
            BulkUpdate operation = this.pendingOperations.asMap().remove(id);

            if (operation == null) {
                Message.BULK_UPDATE_UNKNOWN_ID.send(sender, id);
                return CommandResult.INVALID_ARGS;
            }

            Message.BULK_UPDATE_STARTING.send(sender);
            plugin.getStorage().applyBulkUpdate(operation).whenCompleteAsync((v, ex) -> {
                if (ex == null) {
                    plugin.getSyncTaskBuffer().requestDirectly();
                    Message.BULK_UPDATE_SUCCESS.send(sender);
                } else {
                    ex.printStackTrace();
                    Message.BULK_UPDATE_FAILURE.send(sender);
                }
            }, plugin.getBootstrap().getScheduler().async());
            return CommandResult.SUCCESS;
        }

        if (args.size() < 2) {
            throw new ArgumentParser.DetailedUsageException();
        }

        BulkUpdateBuilder bulkUpdateBuilder = BulkUpdateBuilder.create();

        try {
            bulkUpdateBuilder.dataType(DataType.valueOf(args.remove(0).toUpperCase()));
        } catch (IllegalArgumentException e) {
            Message.BULK_UPDATE_INVALID_DATA_TYPE.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        String action = args.remove(0).toLowerCase();
        switch (action) {
            case "delete":
                bulkUpdateBuilder.action(DeleteAction.create());
                break;
            case "update":
                if (args.size() < 2) {
                    throw new ArgumentParser.DetailedUsageException();
                }

                String field = args.remove(0);
                QueryField queryField = QueryField.of(field);
                if (queryField == null) {
                    throw new ArgumentParser.DetailedUsageException();
                }
                String value = args.remove(0);

                bulkUpdateBuilder.action(UpdateAction.of(queryField, value));
                break;
            default:
                throw new ArgumentParser.DetailedUsageException();
        }

        for (String constraint : args) {
            String[] parts = constraint.split(" ");
            if (parts.length != 3) {
                Message.BULK_UPDATE_INVALID_CONSTRAINT.send(sender, constraint);
                return CommandResult.INVALID_ARGS;
            }

            QueryField field = QueryField.of(parts[0]);
            if (field == null) {
                Message.BULK_UPDATE_INVALID_CONSTRAINT.send(sender, constraint);
                return CommandResult.INVALID_ARGS;
            }

            Comparison comparison = StandardComparison.parseComparison(parts[1]);
            if (comparison == null) {
                Message.BULK_UPDATE_INVALID_COMPARISON.send(sender, parts[1]);
                return CommandResult.INVALID_ARGS;
            }

            String expr = parts[2];
            bulkUpdateBuilder.query(Query.of(field, Constraint.of(comparison, expr)));
        }

        String id = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));

        BulkUpdate bulkUpdate = bulkUpdateBuilder.build();

        this.pendingOperations.put(id, bulkUpdate);

        Message.BULK_UPDATE_QUEUED.send(sender, bulkUpdate.buildAsSql().toReadableString().replace("{table}", bulkUpdate.getDataType().getName()));
        Message.BULK_UPDATE_CONFIRM.send(sender, label, id);

        return CommandResult.SUCCESS;
    }
}
