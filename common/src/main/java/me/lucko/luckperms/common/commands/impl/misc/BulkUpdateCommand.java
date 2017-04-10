/*
 * Copyright (c) 2017 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.commands.impl.misc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.BulkUpdateBuilder;
import me.lucko.luckperms.common.bulkupdate.DataType;
import me.lucko.luckperms.common.bulkupdate.action.DeleteAction;
import me.lucko.luckperms.common.bulkupdate.action.UpdateAction;
import me.lucko.luckperms.common.bulkupdate.comparisons.ComparisonType;
import me.lucko.luckperms.common.bulkupdate.constraint.Constraint;
import me.lucko.luckperms.common.bulkupdate.constraint.QueryField;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class BulkUpdateCommand extends SingleCommand {
    private final Cache<String, BulkUpdate> pendingOperations = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    public BulkUpdateCommand() {
        super("BulkUpdate", "Execute bulk change queries on all data", "/%s bulkupdate", Permission.BULK_UPDATE, Predicates.alwaysFalse(),
                Arg.list(
                        Arg.create("data type", true, "the type of data being changed. ('all', 'users' or 'groups')"),
                        Arg.create("action", true, "the action to perform on the data. ('update' or 'delete')"),
                        Arg.create("action field", false, "the field to act upon. only required for 'update'. ('permission', 'server' or 'world')"),
                        Arg.create("action value", false, "the value to replace with. only required for 'update'."),
                        Arg.create("constraint...", false, "the constraints required for the update")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) throws CommandException {
        if (args.size() == 2 && args.get(0).equalsIgnoreCase("confirm")) {

            String id = args.get(1);
            BulkUpdate operation = pendingOperations.asMap().remove(id);

            if (operation == null) {
                Message.BULK_UPDATE_UNKNOWN_ID.send(sender, id);
                return CommandResult.INVALID_ARGS;
            }

            Message.BULK_UPDATE_STARTING.send(sender);
            plugin.getStorage().applyBulkUpdate(operation).thenAccept(b -> {
                if (b) {
                    Message.BULK_UPDATE_SUCCESS.send(sender);
                } else {
                    Message.BULK_UPDATE_FAILURE.send(sender);
                }
            });
            return CommandResult.SUCCESS;
        }

        if (args.size() < 3) {
            throw new ArgumentUtils.DetailedUsageException();
        }

        BulkUpdateBuilder bulkUpdateBuilder = BulkUpdateBuilder.create();

        try {
            bulkUpdateBuilder.dataType(DataType.valueOf(args.remove(0).toUpperCase()));
        } catch (IllegalArgumentException e) {
            Message.BULK_UPDATE_INVALID_DATA_TYPE.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        String action = args.remove(0).toLowerCase();
        if (action.equals("delete")) {
            bulkUpdateBuilder.action(DeleteAction.create());
        } else if (action.equals("update")) {
            if (args.size() < 2) {
                throw new ArgumentUtils.DetailedUsageException();
            }

            String field = args.remove(0);
            QueryField queryField = QueryField.of(field);
            if (queryField == null) {
                throw new ArgumentUtils.DetailedUsageException();
            }
            String value = args.remove(0);

            bulkUpdateBuilder.action(UpdateAction.of(queryField, value));
        } else {
            throw new ArgumentUtils.DetailedUsageException();
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

            ComparisonType comparison = ComparisonType.parseComparison(parts[1]);
            if (comparison == null) {
                Message.BULK_UPDATE_INVALID_COMPARISON.send(sender, parts[1]);
                return CommandResult.INVALID_ARGS;
            }

            String expr = parts[2];
            bulkUpdateBuilder.constraint(Constraint.of(field, comparison, expr));
        }

        String id = "" + ThreadLocalRandom.current().nextInt(9) +
                ThreadLocalRandom.current().nextInt(9) +
                ThreadLocalRandom.current().nextInt(9) +
                ThreadLocalRandom.current().nextInt(9);

        BulkUpdate bulkUpdate = bulkUpdateBuilder.build();

        pendingOperations.put(id, bulkUpdate);

        Message.BULK_UPDATE_QUEUED.send(sender, bulkUpdate.buildAsSql().replace("{table}", bulkUpdate.getDataType().getName()));
        Message.BULK_UPDATE_CONFIRM.send(sender, label, id);

        return CommandResult.SUCCESS;
    }

    @Override
    public boolean isAuthorized(Sender sender) {
        return sender.isConsole(); // we only want console to be able to use this command
    }
}
