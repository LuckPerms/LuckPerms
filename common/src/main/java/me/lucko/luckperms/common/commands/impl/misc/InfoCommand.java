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

package me.lucko.luckperms.common.commands.impl.misc;

import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.messaging.NoopMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static me.lucko.luckperms.common.commands.utils.Util.formatBoolean;

public class InfoCommand extends SingleCommand {
    private static String formatValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Util.formatBoolean(Boolean.parseBoolean(value));
        }

        try {
            int i = Integer.parseInt(value);
            return "&a" + i;
        } catch (NumberFormatException ignored) {}

        return "&f" + value;
    }

    public InfoCommand() {
        super("Info", "Print general plugin info", "/%s info", Permission.INFO, Predicates.alwaysFalse(), null);
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        final LuckPermsConfiguration c = plugin.getConfiguration();
        Message.INFO.send(sender,
                plugin.getVersion(),
                plugin.getServerType().getFriendlyName(),
                plugin.getStorage().getName(),
                c.get(ConfigKeys.SERVER),
                c.get(ConfigKeys.SYNC_TIME),
                plugin.getMessagingService() instanceof NoopMessagingService ? "None" : plugin.getMessagingService().getName(),
                plugin.getPlayerCount(),
                plugin.getUserManager().getAll().size(),
                plugin.getGroupManager().getAll().size(),
                plugin.getTrackManager().getAll().size(),
                plugin.getStorage().getLog().join().getContent().size(),
                plugin.getUuidCache().getSize(),
                plugin.getLocaleManager().getSize(),
                plugin.getPreProcessContexts(false).size(),
                plugin.getContextManager().getCalculatorsSize(),
                plugin.getPermissionVault().getSize(),
                formatBoolean(c.get(ConfigKeys.USE_SERVER_UUIDS)),
                formatBoolean(c.get(ConfigKeys.INCLUDING_GLOBAL_PERMS)),
                formatBoolean(c.get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS)),
                formatBoolean(c.get(ConfigKeys.APPLYING_GLOBAL_GROUPS)),
                formatBoolean(c.get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS)),
                formatBoolean(c.get(ConfigKeys.APPLYING_WILDCARDS)),
                formatBoolean(c.get(ConfigKeys.APPLYING_REGEX)),
                formatBoolean(c.get(ConfigKeys.APPLYING_SHORTHAND))
        );

        LinkedHashMap<String, Object> platformInfo = plugin.getExtraInfo();
        if (platformInfo == null || platformInfo.isEmpty()) {
            return CommandResult.SUCCESS;
        }

        Message.EMPTY.send(sender, "&f-  &bPlatform Info:");
        for (Map.Entry<String, Object> e : platformInfo.entrySet()) {
            Message.EMPTY.send(sender, "&f-     &3" + e.getKey() + ": " + formatValue(e.getValue().toString()));
        }

        return CommandResult.SUCCESS;
    }
}
