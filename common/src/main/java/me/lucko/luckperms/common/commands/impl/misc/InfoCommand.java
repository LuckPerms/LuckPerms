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

package me.lucko.luckperms.common.commands.impl.misc;

import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InfoCommand extends SingleCommand {
    public InfoCommand(LocaleManager locale) {
        super(CommandSpec.INFO.spec(locale), "Info", CommandPermission.INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        final LuckPermsConfiguration c = plugin.getConfiguration();

        Message.INFO_TOP.send(sender,
                plugin.getVersion(),
                plugin.getServerType().getFriendlyName(),
                plugin.getServerBrand(),
                plugin.getServerVersion()
        );

        Map<String, String> storageInfo = plugin.getStorage().getMeta();

        Message.EMPTY.send(sender, "&f-  &bStorage:");
        Message.EMPTY.send(sender, "&f-     &3Type: &f" + plugin.getStorage().getName());
        for (Map.Entry<String, String> e : storageInfo.entrySet()) {
            Message.EMPTY.send(sender, "&f-     &3" + e.getKey() + ": " + formatValue(e.getValue()));
        }

        Message.INFO_MIDDLE.send(sender,
                plugin.getMessagingService().map(ExtendedMessagingService::getName).orElse("None"),
                plugin.getContextManager().getStaticContextString().orElse("None"),
                plugin.getPlayerCount(),
                plugin.getUniqueConnections().size(),
                DateUtil.formatTimeBrief((System.currentTimeMillis() - plugin.getStartTime()) / 1000L),
                plugin.getUserManager().getAll().size(),
                plugin.getGroupManager().getAll().size(),
                plugin.getTrackManager().getAll().size(),
                plugin.getContextManager().getCalculatorsSize(),
                plugin.getPermissionVault().getSize(),
                plugin.getCalculatorFactory().getActiveProcessors().stream().collect(Collectors.joining(", "))
        );

        Map<String, Object> platformInfo = plugin.getExtraInfo();
        if (!platformInfo.isEmpty()) {
            Message.EMPTY.send(sender, "&f-  &bPlatform Info:");
            for (Map.Entry<String, Object> e : platformInfo.entrySet()) {
                Message.EMPTY.send(sender, "&f-     &3" + e.getKey() + ": " + formatValue(e.getValue().toString()));
            }
        }

        return CommandResult.SUCCESS;
    }

    private static String formatValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return CommandUtils.formatBoolean(Boolean.parseBoolean(value));
        }

        try {
            int i = Integer.parseInt(value);
            return "&a" + i;
        } catch (NumberFormatException ignored) {}

        return "&f" + value;
    }
}
