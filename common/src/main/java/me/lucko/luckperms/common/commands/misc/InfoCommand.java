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

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.DurationFormatter;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.extension.Extension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class InfoCommand extends SingleCommand {
    public InfoCommand(LocaleManager locale) {
        super(CommandSpec.INFO.localize(locale), "Info", CommandPermission.INFO, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        Map<String, String> storageMeta = plugin.getStorage().getMeta();

        Message.INFO_TOP.send(sender,
                plugin.getBootstrap().getVersion(),
                plugin.getBootstrap().getType().getFriendlyName(),
                plugin.getBootstrap().getServerBrand(),
                plugin.getBootstrap().getServerVersion()
        );

        Message.INFO_STORAGE.send(sender, plugin.getStorage().getName());
        for (Map.Entry<String, String> e : storageMeta.entrySet()) {
            Message.INFO_STORAGE_META.send(sender, e.getKey(), formatValue(e.getValue()));
        }

        Collection<Extension> loadedExtensions = plugin.getExtensionManager().getLoadedExtensions();
        if (!loadedExtensions.isEmpty()) {
            Message.INFO_EXTENSIONS.send(sender);
            for (Extension extension : loadedExtensions) {
                Message.INFO_EXTENSION_ENTRY.send(sender, extension.getClass().getName());
            }
        }

        ImmutableContextSet staticContext = plugin.getContextManager().getStaticContext();
        Message.INFO_MIDDLE.send(sender,
                plugin.getMessagingService().map(InternalMessagingService::getName).orElse("None"),
                staticContext.isEmpty() ? "None" : MessageUtils.contextSetToString(plugin.getLocaleManager(), staticContext),
                plugin.getBootstrap().getPlayerCount(),
                plugin.getConnectionListener().getUniqueConnections().size(),
                DurationFormatter.CONCISE_LOW_ACCURACY.format(Duration.between(plugin.getBootstrap().getStartupTime(), Instant.now())),
                plugin.getUserManager().getAll().size(),
                plugin.getGroupManager().getAll().size(),
                plugin.getTrackManager().getAll().size()
        );

        return CommandResult.SUCCESS;
    }

    private static String formatValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return MessageUtils.formatBoolean(Boolean.parseBoolean(value));
        }

        try {
            int i = Integer.parseInt(value);
            return "&a" + i;
        } catch (NumberFormatException ignored) {}

        return "&f" + value;
    }
}
