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

package me.lucko.luckperms.common.commands.generic.permission;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.abstraction.GenericChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.SignedDuration;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.types.InheritanceNode;

import java.time.Duration;
import java.util.List;

public class PermissionUnsetTemp extends GenericChildCommand {
    public PermissionUnsetTemp() {
        super(CommandSpec.PERMISSION_UNSETTEMP, "unsettemp", CommandPermission.USER_PERM_UNSET_TEMP, CommandPermission.GROUP_PERM_UNSET_TEMP, Predicates.is(0));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String node = args.get(0);
        SignedDuration duration = args.getSignedDurationOrDefault(1, null);
        int fromIndex = duration == null ? 1 : 2;
        MutableContextSet context = args.getContextOrDefault(fromIndex, plugin);

        if (node.isEmpty()) {
            Message.INVALID_PERMISSION_EMPTY.send(sender);
            return;
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, node)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Node builtNode = NodeBuilders.determineMostApplicable(node).expiry(10L).withContext(context).build();

        if (builtNode instanceof InheritanceNode) {
            if (ArgumentPermissions.checkGroup(plugin, sender, ((InheritanceNode) builtNode).getGroupName(), context)) {
                Message.COMMAND_NO_PERMISSION.send(sender);
                return;
            }
        }

        // no duration - remove an existing temporary node outright
        if (duration == null) {
            remove(plugin, sender, target, node, builtNode, null, context);
            return;
        }

        switch (duration.sign()) {
            case SUBTRACT:
                remove(plugin, sender, target, node, builtNode, duration.duration(), context);
                return;
            case ABSOLUTE:
                // a permission that is already temporary gets shortened, as it always has done.
                // anything else - permanent, or not set here at all - is taken away for a while instead.
                if (findTemporaryNode(target, builtNode, context) != null) {
                    remove(plugin, sender, target, node, builtNode, duration.duration(), context);
                } else {
                    revoke(plugin, sender, target, node, context, duration.duration());
                }
                return;
            case ADD:
                extendRevocation(plugin, sender, target, node, builtNode, context, duration.duration());
                return;
            default:
                throw new AssertionError(duration.sign());
        }
    }

    /**
     * Finds the temporary node the holder already has for this permission in this context,
     * whatever its value, or null if there isn't one.
     */
    private static Node findTemporaryNode(PermissionHolder target, Node builtNode, MutableContextSet context) {
        // builtNode carries an expiry, and IGNORE_EXPIRY_TIME_AND_VALUE still compares whether
        // a node has one, so permanent nodes are never matched here
        return target.getData(DataType.NORMAL).nodesInContext(context).stream()
                .filter(NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE.equalTo(builtNode))
                .findFirst().orElse(null);
    }

    /**
     * Removes an existing temporary node, or - if a duration is given - takes that duration
     * away from its expiry, only removing it if that would take it into the past.
     */
    private static void remove(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, String node, Node builtNode, Duration duration, MutableContextSet context) {
        DataMutateResult.WithMergedNode result = target.unsetNode(DataType.NORMAL, builtNode, duration);
        if (!result.getResult().wasSuccessful()) {
            Message.DOES_NOT_HAVE_TEMP_PERMISSION.send(sender, target, node, context);
            return;
        }

        Node mergedNode = result.getMergedNode();
        //noinspection ConstantConditions
        if (mergedNode != null) {
            Message.UNSET_TEMP_PERMISSION_SUBTRACT_SUCCESS.send(sender, mergedNode.getKey(), mergedNode.getValue(), target, mergedNode.getExpiryDuration(), context, duration);

            LoggedAction.build().source(sender).target(target)
                    .description("permission", "unsettemp", node, "-" + duration, context)
                    .build().submit(plugin, sender);
        } else {
            Message.UNSET_TEMP_PERMISSION_SUCCESS.send(sender, node, target, context);

            LoggedAction.build().source(sender).target(target)
                    .description("permission", "unsettemp", node, context)
                    .build().submit(plugin, sender);
        }

        StorageAssistant.save(target, sender, plugin);
    }

    /**
     * Temporarily takes a permission away, letting it come back on its own once the
     * duration elapses.
     *
     * <p>This works by adding a temporary negated node rather than by deleting anything and
     * scheduling a restore - temporary nodes take priority over permanent ones (see
     * {@code NodeComparator}), so the negation wins for as long as it lives, and whatever was
     * there before simply applies again once it expires. That keeps the whole thing in the
     * holder's data, so it survives restarts and syncs to other servers like any other node.</p>
     *
     * <p>The caller only routes here when the holder has no temporary node for this permission
     * already. Where it does, the duration keeps its long standing meaning of shortening that
     * node instead.</p>
     */
    private static void revoke(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, String node, MutableContextSet context, Duration duration) {
        Node negatedNode = NodeBuilders.determineMostApplicable(node).value(false).withContext(context).expiry(duration).build();
        applyRevocation(plugin, sender, target, node, context, negatedNode, TemporaryNodeMergeStrategy.NONE);
    }

    /**
     * Adds more time to a temporary removal that is already in place.
     */
    private static void extendRevocation(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, String node, Node builtNode, MutableContextSet context, Duration duration) {
        Node existing = findTemporaryNode(target, builtNode, context);
        if (existing == null || existing.getValue()) {
            Message.NO_TEMP_PERMISSION_REVOCATION.send(sender, target, node, context);
            return;
        }

        Node negatedNode = NodeBuilders.determineMostApplicable(node).value(false).withContext(context).expiry(duration).build();
        applyRevocation(plugin, sender, target, node, context, negatedNode, TemporaryNodeMergeStrategy.ADD_NEW_DURATION_TO_EXISTING);
    }

    private static void applyRevocation(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, String node, MutableContextSet context, Node negatedNode, TemporaryNodeMergeStrategy strategy) {
        DataMutateResult.WithMergedNode result = target.setNode(DataType.NORMAL, negatedNode, strategy);
        if (!result.getResult().wasSuccessful()) {
            Message.ALREADY_HAS_TEMP_PERMISSION.send(sender, target, node, context);
            return;
        }

        Duration totalDuration = result.getMergedNode().getExpiryDuration();
        Message.UNSET_TEMP_PERMISSION_REVOKE_SUCCESS.send(sender, node, target, totalDuration, context);

        LoggedAction.build().source(sender).target(target)
                .description("permission", "unsettemp", node, totalDuration, context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.permissions(plugin))
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
